package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.common.NotFoundException;
import com.example.webrtcbackend.transcription.dto.AssignmentResponse;
import com.example.webrtcbackend.transcription.dto.CreateAssignmentRequest;
import com.example.webrtcbackend.transcription.dto.QuestionDTO;
import com.example.webrtcbackend.transcription.dto.UpdateQuestionRequest;
import com.example.webrtcbackend.video.VideoMetadata;
import com.example.webrtcbackend.video.VideoMetadataRepository;
import com.example.webrtcbackend.video.VideoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final TranscriptionService transcriptionService;
    private final GeminiService geminiService;
    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoService videoService;
    private final TranscriptRepository transcriptRepository;
    private final ObjectMapper objectMapper;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             QuestionRepository questionRepository,
                             OptionRepository optionRepository,
                             TranscriptionService transcriptionService,
                             GeminiService geminiService,
                             VideoMetadataRepository videoMetadataRepository,
                             VideoService videoService,
                             TranscriptRepository transcriptRepository) {
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.transcriptionService = transcriptionService;
        this.geminiService = geminiService;
        this.videoMetadataRepository = videoMetadataRepository;
        this.videoService = videoService;
        this.transcriptRepository = transcriptRepository;
        this.objectMapper = new ObjectMapper();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CREATE — Synchronous pipeline (transcribe + AI + persist)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new assignment by synchronously processing the full pipeline:
     *   1. Save assignment entity (status = PROCESSING)
     *   2. Find video for the lesson
     *   3. Transcribe video via AssemblyAI
     *   4. Generate questions via Gemini AI
     *   5. Persist questions + options to DB
     *   6. Update status to DONE and return full response with questions
     *
     * This is a blocking call that may take 1-5 minutes depending on video length.
     */
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        log.info("Creating assignment synchronously for lessonId={}, title='{}'",
                request.getLessonId(), request.getTitle());

        // 1. Create and save assignment
        Assignment assignment = new Assignment();
        assignment.setLessonId(request.getLessonId());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setStatusProgress("PROCESSING");
        assignment.setRetryCount(0);
        assignment.setCreatedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        try {
            // 2. Find videos for the lesson
            List<VideoMetadata> videos = videoMetadataRepository.findByLessonId(request.getLessonId());
            if (videos.isEmpty()) {
                throw new RuntimeException("Không tìm thấy video nào cho bài học này");
            }

            // 3. Transcribe the video
            VideoMetadata video = videos.get(0);
            String videoUrl = videoService.generateVideoUrl(video.getObjectKey());
            log.info("Transcribing video objectKey={}", video.getObjectKey());

            String transcriptText = transcriptionService.transcribeForWorker(videoUrl, String.valueOf(video.getId()));
            if (transcriptText == null || transcriptText.isBlank()) {
                throw new RuntimeException("Transcription trả về kết quả rỗng");
            }

            // Save transcript with assignment linkage
            Transcript transcript = new Transcript();
            transcript.setAssemblyTranscriptId("sync-" + assignment.getId());
            transcript.setVideoUrl(videoUrl);
            transcript.setVideoId(String.valueOf(video.getId()));
            transcript.setAssignmentId(assignment.getId());
            transcript.setStatus("completed");
            transcript.setTranscriptText(transcriptText);
            transcript.setCreatedAt(Instant.now());
            transcript.setCompletedAt(Instant.now());
            transcriptRepository.save(transcript);

            log.info("Transcription completed, length={}", transcriptText.length());

            // 4. Generate questions via Gemini AI
            String rawQuestions = geminiService.generateQuestions(transcriptText);
            if (rawQuestions == null || !rawQuestions.trim().startsWith("[")) {
                throw new RuntimeException("Gemini AI trả về kết quả không hợp lệ");
            }

            List<QuestionDTO> questionDTOs = objectMapper.readValue(
                    rawQuestions, new TypeReference<List<QuestionDTO>>() {});

            log.info("Generated {} questions via AI", questionDTOs.size());

            // 5. Persist questions and options
            saveQuestionsAndOptions(assignment.getId(), questionDTOs);

            // 6. Update status to DONE
            assignment.setStatusProgress("DONE");
            assignment = assignmentRepository.save(assignment);

            log.info("Assignment created successfully id={}, status=DONE", assignment.getId());

            // Return full response with questions
            List<Question> savedQuestions = questionRepository.findByAssignmentId(assignment.getId());
            List<QuestionDTO> savedDTOs = buildQuestionDTOs(savedQuestions);
            return toResponse(assignment, savedDTOs);

        } catch (Exception e) {
            log.error("Assignment creation failed for id={}: {}", assignment.getId(), e.getMessage(), e);
            assignment.setStatusProgress("FAILED");
            assignment.setErrorMessage(e.getMessage());
            assignmentRepository.save(assignment);
            return toResponse(assignment, Collections.emptyList());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  READ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get assignment by ID with questions and options.
     */
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + id));

        List<Question> questions = questionRepository.findByAssignmentId(id);
        List<QuestionDTO> questionDTOs = buildQuestionDTOs(questions);

        return toResponse(assignment, questionDTOs);
    }

    /**
     * Get all assignments for a lesson with questions and options.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByLessonId(Long lessonId) {
        List<Assignment> assignments = assignmentRepository.findByLessonId(lessonId);

        return assignments.stream().map(assignment -> {
            List<Question> questions = questionRepository.findByAssignmentId(assignment.getId());
            List<QuestionDTO> questionDTOs = buildQuestionDTOs(questions);
            return toResponse(assignment, questionDTOs);
        }).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UPDATE QUESTION — Lecturer can edit question text + options
    // ═══════════════════════════════════════════════════════════════

    /**
     * Update a question's text, correct answer, and options.
     */
    @Transactional
    public QuestionDTO updateQuestion(Long assignmentId, Long questionId, UpdateQuestionRequest request) {
        // Verify assignment exists
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        // Find and update question
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found with id=" + questionId));

        if (!question.getAssignmentId().equals(assignmentId)) {
            throw new IllegalArgumentException("Question does not belong to this assignment");
        }

        question.setQuestion(request.getQuestion());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question = questionRepository.save(question);

        // Replace options: delete old, insert new
        optionRepository.deleteByQuestionId(questionId);

        if (request.getOptions() != null) {
            for (Map.Entry<String, String> entry : request.getOptions().entrySet()) {
                Option option = new Option();
                option.setQuestionId(questionId);
                option.setOptionKey(entry.getKey());
                option.setOptionValue(entry.getValue());
                optionRepository.save(option);
            }
        }

        // Build and return updated DTO
        List<Option> updatedOptions = optionRepository.findByQuestionId(questionId);
        Map<String, String> optionsMap = new LinkedHashMap<>();
        for (Option opt : updatedOptions) {
            optionsMap.put(opt.getOptionKey(), opt.getOptionValue());
        }

        return new QuestionDTO(question.getId(), question.getQuestion(), optionsMap, question.getCorrectAnswer());
    }

    // ═══════════════════════════════════════════════════════════════
    //  DELETE QUESTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Delete a question and its options from an assignment.
     */
    @Transactional
    public void deleteQuestion(Long assignmentId, Long questionId) {
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found with id=" + questionId));

        if (!question.getAssignmentId().equals(assignmentId)) {
            throw new IllegalArgumentException("Question does not belong to this assignment");
        }

        optionRepository.deleteByQuestionId(questionId);
        questionRepository.delete(question);

        log.info("Deleted question id={} from assignment id={}", questionId, assignmentId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save questions and options to DB.
     */
    private void saveQuestionsAndOptions(Long assignmentId, List<QuestionDTO> questionDTOs) {
        for (QuestionDTO dto : questionDTOs) {
            Question question = new Question();
            question.setAssignmentId(assignmentId);
            question.setQuestion(dto.getQuestion());
            question.setCorrectAnswer(dto.getAnswer());
            question = questionRepository.save(question);

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

        log.info("Saved {} questions with options for assignmentId={}", questionDTOs.size(), assignmentId);
    }

    /**
     * Build QuestionDTO list with options from DB entities.
     */
    private List<QuestionDTO> buildQuestionDTOs(List<Question> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        Map<Long, List<Option>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        return questions.stream().map(q -> {
            Map<String, String> optionsMap = new LinkedHashMap<>();
            List<Option> questionOptions = optionsByQuestion.getOrDefault(q.getId(), Collections.emptyList());
            for (Option opt : questionOptions) {
                optionsMap.put(opt.getOptionKey(), opt.getOptionValue());
            }
            return new QuestionDTO(q.getId(), q.getQuestion(), optionsMap, q.getCorrectAnswer());
        }).toList();
    }

    private AssignmentResponse toResponse(Assignment assignment, List<QuestionDTO> questions) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setLessonId(assignment.getLessonId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setStatusProgress(assignment.getStatusProgress());
        response.setErrorMessage(assignment.getErrorMessage());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setQuestions(questions);
        return response;
    }
}
