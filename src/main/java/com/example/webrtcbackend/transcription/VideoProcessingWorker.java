package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.video.VideoMetadata;
import com.example.webrtcbackend.video.VideoMetadataRepository;
import com.example.webrtcbackend.video.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Background worker that continuously consumes from the "assignment-queue"
 * Redis queue and processes video transcription for each assignment.
 *
 * For each assignmentId:
 *   1. Load assignment from DB
 *   2. If status is not PROCESSING → skip
 *   3. Find videos for the lesson
 *   4. Generate presigned URL and transcribe via AssemblyAI
 *   5. Save transcript to DB
 *   6. Push assignmentId to "ai-queue"
 *
 * On error → push to retry queue.
 */
@Component
public class VideoProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingWorker.class);

    private final RedisQueueService redisQueueService;
    private final AssignmentRepository assignmentRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptionService transcriptionService;
    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoService videoService;

    private volatile boolean running = true;

    public VideoProcessingWorker(RedisQueueService redisQueueService,
                                 AssignmentRepository assignmentRepository,
                                 TranscriptRepository transcriptRepository,
                                 TranscriptionService transcriptionService,
                                 VideoMetadataRepository videoMetadataRepository,
                                 VideoService videoService) {
        this.redisQueueService = redisQueueService;
        this.assignmentRepository = assignmentRepository;
        this.transcriptRepository = transcriptRepository;
        this.transcriptionService = transcriptionService;
        this.videoMetadataRepository = videoMetadataRepository;
        this.videoService = videoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        Thread worker = new Thread(this::processQueue, "video-processing-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("VideoProcessingWorker started – listening on queue='{}'", RedisQueueService.ASSIGNMENT_QUEUE);
    }

    private void processQueue() {
        while (running) {
            try {
                // Blocking pop with 5-second timeout
                String value = redisQueueService.blockingPop(
                        RedisQueueService.ASSIGNMENT_QUEUE,
                        Duration.ofSeconds(5)
                );

                if (value == null) {
                    // Timeout elapsed, no item available; loop back and wait again
                    continue;
                }

                Long assignmentId = Long.parseLong(value);
                log.info("[VideoWorker] Job received – assignmentId={}", assignmentId);

                processAssignment(assignmentId);

            } catch (Exception e) {
                log.error("[VideoWorker] Unexpected error in queue loop", e);
                // Brief pause before retrying the loop to avoid tight-looping on persistent errors
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processAssignment(Long assignmentId) {
        try {
            // 1. Load assignment
            Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
            if (assignment == null) {
                log.warn("[VideoWorker] Assignment not found – id={}, skipping", assignmentId);
                return;
            }

            // 2. Check status – only process if PROCESSING
            if (!"PROCESSING".equals(assignment.getStatusProgress())) {
                log.info("[VideoWorker] Assignment id={} has status='{}', skipping",
                        assignmentId, assignment.getStatusProgress());
                return;
            }

            log.info("[VideoWorker] Processing started – assignmentId={}", assignmentId);

            // 3. Find videos for this lesson
            List<VideoMetadata> videos = videoMetadataRepository
                    .findByLessonId(assignment.getLessonId());

            if (videos.isEmpty()) {
                log.warn("[VideoWorker] No videos found for lessonId={}", assignment.getLessonId());
                assignment.setStatusProgress("FAILED");
                assignment.setErrorMessage("No videos found for lesson");
                assignmentRepository.save(assignment);
                return;
            }

            // 4. Take the first video and generate presigned URL for transcription
            VideoMetadata video = videos.get(0);
            String videoUrl = videoService.generateVideoUrl(video.getObjectKey());
            log.info("[VideoWorker] Generated video URL for objectKey={}", video.getObjectKey());

            // 5. Transcribe the video using existing TranscriptionService
            String transcriptText = transcriptionService.transcribeForWorker(videoUrl, String.valueOf(video.getId()));

            if (transcriptText == null || transcriptText.isBlank()) {
                throw new RuntimeException("Transcription returned empty text");
            }

            // 6. Save/update transcript with assignmentId linkage
            Transcript transcript = new Transcript();
            transcript.setAssemblyTranscriptId("worker-" + assignmentId);
            transcript.setVideoUrl(videoUrl);
            transcript.setVideoId(String.valueOf(video.getId()));
            transcript.setAssignmentId(assignmentId);
            transcript.setStatus("completed");
            transcript.setTranscriptText(transcriptText);
            transcript.setCreatedAt(java.time.Instant.now());
            transcript.setCompletedAt(java.time.Instant.now());
            transcriptRepository.save(transcript);

            log.info("[VideoWorker] Processing success – assignmentId={}, transcriptLength={}",
                    assignmentId, transcriptText.length());

            // 7. Push to AI queue for question generation
            redisQueueService.pushToAiQueue(assignmentId);

        } catch (Exception e) {
            log.error("[VideoWorker] Processing failed – assignmentId={}", assignmentId, e);
            // Push to retry queue on error
            redisQueueService.pushToRetryQueue(assignmentId);
        }
    }

    public void stop() {
        this.running = false;
    }
}
