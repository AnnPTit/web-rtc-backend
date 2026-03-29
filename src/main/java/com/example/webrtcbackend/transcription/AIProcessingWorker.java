package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.transcription.dto.QuestionDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Background worker that continuously consumes from the "ai-queue"
 * Redis queue and generates questions from transcripts using AI (Gemini).
 *
 * For each assignmentId:
 *   1. Load transcript for the assignment
 *   2. Call Gemini AI to generate questions with A/B/C/D options
 *   3. Persist questions and options to DB
 *   4. Update assignment status to DONE
 *
 * On error:
 *   - Push to retry queue
 *   - If retry limit exceeded → set assignment status = FAILED
 */
@Component
public class AIProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(AIProcessingWorker.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final RedisQueueService redisQueueService;
    private final AssignmentRepository assignmentRepository;
    private final TranscriptRepository transcriptRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private volatile boolean running = true;

    public AIProcessingWorker(RedisQueueService redisQueueService,
                              AssignmentRepository assignmentRepository,
                              TranscriptRepository transcriptRepository,
                              QuestionRepository questionRepository,
                              OptionRepository optionRepository,
                              GeminiService geminiService) {
        this.redisQueueService = redisQueueService;
        this.assignmentRepository = assignmentRepository;
        this.transcriptRepository = transcriptRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.geminiService = geminiService;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        Thread worker = new Thread(this::processQueue, "ai-processing-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("AIProcessingWorker started – listening on queue='{}'", RedisQueueService.AI_QUEUE);
    }

    private void processQueue() {
        while (running) {
            try {
                // Blocking pop with 5-second timeout
                String value = redisQueueService.blockingPop(
                        RedisQueueService.AI_QUEUE,
                        Duration.ofSeconds(5)
                );

                if (value == null) {
                    continue;
                }

                Long assignmentId = Long.parseLong(value);
                log.info("[AIWorker] Job received – assignmentId={}", assignmentId);

                processAI(assignmentId);

            } catch (Exception e) {
                log.error("[AIWorker] Unexpected error in queue loop", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processAI(Long assignmentId) {
        try {
            // 1. Load assignment
            Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
            if (assignment == null) {
                log.warn("[AIWorker] Assignment not found – id={}, skipping", assignmentId);
                return;
            }

            // Check status – only process if PROCESSING
            if (!"PROCESSING".equals(assignment.getStatusProgress())) {
                log.info("[AIWorker] Assignment id={} has status='{}', skipping",
                        assignmentId, assignment.getStatusProgress());
                return;
            }

            log.info("[AIWorker] Processing started – assignmentId={}", assignmentId);

            // 2. Load transcript for this assignment
            String transcriptText = findTranscriptText(assignmentId, assignment.getLessonId());

            if (transcriptText == null || transcriptText.isBlank()) {
                throw new RuntimeException("No transcript text found for assignmentId=" + assignmentId);
            }

            // 3. Call Gemini AI to generate questions
            String rawQuestions = geminiService.generateQuestions(transcriptText);

            if (rawQuestions == null || !rawQuestions.trim().startsWith("[")) {
                throw new RuntimeException("Invalid Gemini AI response: " + rawQuestions);
            }

            List<QuestionDTO> questionDTOs = objectMapper.readValue(
                    rawQuestions,
                    new TypeReference<List<QuestionDTO>>() {}
            );

            log.info("[AIWorker] Generated {} questions for assignmentId={}", questionDTOs.size(), assignmentId);

            // 4. Persist questions and options (transactional)
            saveQuestionsAndOptions(assignmentId, questionDTOs);

            // 5. Update assignment status to DONE
            assignment.setStatusProgress("DONE");
            assignmentRepository.save(assignment);

            log.info("[AIWorker] Processing success – assignmentId={}, status=DONE", assignmentId);

        } catch (Exception e) {
            log.error("[AIWorker] Processing failed – assignmentId={}", assignmentId, e);
            handleRetry(assignmentId, e.getMessage());
        }
    }

    /**
     * Find transcript text for an assignment.
     * First tries by assignmentId, then falls back to finding by lessonId's videos.
     */
    private String findTranscriptText(Long assignmentId, Long lessonId) {
        // Try to find transcript linked to this assignment
        List<Transcript> transcripts = transcriptRepository.findByAssignmentId(assignmentId);
        if (!transcripts.isEmpty()) {
            return transcripts.get(0).getTranscriptText();
        }

        // Fallback: find any transcript by videoId that might belong to this lesson
        log.warn("[AIWorker] No transcript found by assignmentId={}, searching broader", assignmentId);
        return null;
    }

    /**
     * Save questions and options to DB transactionally.
     */
    @Transactional
    public void saveQuestionsAndOptions(Long assignmentId, List<QuestionDTO> questionDTOs) {
        for (QuestionDTO dto : questionDTOs) {
            // Save question
            Question question = new Question();
            question.setAssignmentId(assignmentId);
            question.setQuestion(dto.getQuestion());
            question.setCorrectAnswer(dto.getAnswer());
            question = questionRepository.save(question);

            // Save options
            if (dto.getOptions() != null) {
                for (Map.Entry<String, String> entry : dto.getOptions().entrySet()) {
                    Option option = new Option();
                    option.setQuestionId(question.getId());
                    option.setOptionKey(entry.getKey());
                    option.setOptionValue(entry.getValue());
                    optionRepository.save(option);
                }
            }
        }

        log.info("[AIWorker] Saved {} questions with options for assignmentId={}",
                questionDTOs.size(), assignmentId);
    }

    /**
     * Handle retry logic:
     * - If retry < 3: push back to retry queue
     * - If retry >= 3: mark assignment as FAILED
     */
    private void handleRetry(Long assignmentId, String errorMessage) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            log.error("[AIWorker] Cannot retry – assignment not found id={}", assignmentId);
            return;
        }

        int retryCount = assignment.getRetryCount() != null ? assignment.getRetryCount() : 0;
        retryCount++;
        assignment.setRetryCount(retryCount);

        if (retryCount < MAX_RETRY_COUNT) {
            log.warn("[AIWorker] Retrying assignmentId={} (attempt {}/{})",
                    assignmentId, retryCount, MAX_RETRY_COUNT);
            assignment.setErrorMessage("Retry " + retryCount + ": " + errorMessage);
            assignmentRepository.save(assignment);
            redisQueueService.pushToRetryQueue(assignmentId);
        } else {
            log.error("[AIWorker] Max retries exceeded for assignmentId={}, marking as FAILED",
                    assignmentId);
            assignment.setStatusProgress("FAILED");
            assignment.setErrorMessage("Max retries exceeded. Last error: " + errorMessage);
            assignmentRepository.save(assignment);
        }
    }

    public void stop() {
        this.running = false;
    }
}
