package com.example.webrtcbackend.transcription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Background worker that consumes from the "retry-queue" Redis queue.
 *
 * Retry logic:
 *   - If assignment retry count < 3: re-queue to assignment-queue for reprocessing
 *   - If assignment retry count >= 3: mark assignment as FAILED with error message
 */
@Component
public class RetryWorker {

    private static final Logger log = LoggerFactory.getLogger(RetryWorker.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final RedisQueueService redisQueueService;
    private final AssignmentRepository assignmentRepository;

    private volatile boolean running = true;

    public RetryWorker(RedisQueueService redisQueueService,
                       AssignmentRepository assignmentRepository) {
        this.redisQueueService = redisQueueService;
        this.assignmentRepository = assignmentRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        Thread worker = new Thread(this::processQueue, "retry-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("RetryWorker started – listening on queue='{}'", RedisQueueService.RETRY_QUEUE);
    }

    private void processQueue() {
        while (running) {
            try {
                String value = redisQueueService.blockingPop(
                        RedisQueueService.RETRY_QUEUE,
                        Duration.ofSeconds(5)
                );

                if (value == null) {
                    continue;
                }

                Long assignmentId = Long.parseLong(value);
                log.info("[RetryWorker] Job received – assignmentId={}", assignmentId);

                handleRetry(assignmentId);

            } catch (Exception e) {
                log.error("[RetryWorker] Unexpected error in queue loop", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handleRetry(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            log.warn("[RetryWorker] Assignment not found – id={}, skipping", assignmentId);
            return;
        }

        // Don't retry if already completed or failed
        if ("DONE".equals(assignment.getStatusProgress()) || "FAILED".equals(assignment.getStatusProgress())) {
            log.info("[RetryWorker] Assignment id={} has status='{}', skipping retry",
                    assignmentId, assignment.getStatusProgress());
            return;
        }

        int retryCount = assignment.getRetryCount() != null ? assignment.getRetryCount() : 0;
        retryCount++;
        assignment.setRetryCount(retryCount);

        if (retryCount < MAX_RETRY_COUNT) {
            log.info("[RetryWorker] Re-queueing assignmentId={} (retry {}/{})",
                    assignmentId, retryCount, MAX_RETRY_COUNT);
            assignmentRepository.save(assignment);

            // Re-queue to the assignment processing queue
            redisQueueService.pushToAssignmentQueue(assignmentId);
        } else {
            log.error("[RetryWorker] Max retries exceeded for assignmentId={}, marking as FAILED",
                    assignmentId);
            assignment.setStatusProgress("FAILED");
            assignment.setErrorMessage("Processing failed after " + MAX_RETRY_COUNT + " retries");
            assignmentRepository.save(assignment);
        }
    }

    public void stop() {
        this.running = false;
    }
}
