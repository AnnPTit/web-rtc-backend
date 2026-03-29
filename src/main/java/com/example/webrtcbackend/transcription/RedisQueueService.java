package com.example.webrtcbackend.transcription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Helper service for Redis-based queue operations.
 * Uses Redis LIST data structure with blocking pop for reliable queue consumption.
 */
@Service
public class RedisQueueService {

    private static final Logger log = LoggerFactory.getLogger(RedisQueueService.class);

    public static final String ASSIGNMENT_QUEUE = "assignment-queue";
    public static final String AI_QUEUE = "ai-queue";
    public static final String RETRY_QUEUE = "retry-queue";

    private final StringRedisTemplate redisTemplate;

    public RedisQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Push an assignmentId to the assignment processing queue.
     */
    public void pushToAssignmentQueue(Long assignmentId) {
        String value = String.valueOf(assignmentId);
        redisTemplate.opsForList().rightPush(ASSIGNMENT_QUEUE, value);
        log.info("Pushed assignmentId={} to queue={}", assignmentId, ASSIGNMENT_QUEUE);
    }

    /**
     * Push an assignmentId to the AI processing queue.
     */
    public void pushToAiQueue(Long assignmentId) {
        String value = String.valueOf(assignmentId);
        redisTemplate.opsForList().rightPush(AI_QUEUE, value);
        log.info("Pushed assignmentId={} to queue={}", assignmentId, AI_QUEUE);
    }

    /**
     * Push an assignmentId to the retry queue.
     */
    public void pushToRetryQueue(Long assignmentId) {
        String value = String.valueOf(assignmentId);
        redisTemplate.opsForList().rightPush(RETRY_QUEUE, value);
        log.info("Pushed assignmentId={} to queue={}", assignmentId, RETRY_QUEUE);
    }

    /**
     * Blocking pop from the specified queue.
     * Blocks for up to the specified timeout waiting for an element.
     *
     * @return the assignmentId as a String, or null if timeout elapsed
     */
    public String blockingPop(String queueName, Duration timeout) {
        return redisTemplate.opsForList().leftPop(queueName, timeout);
    }
}
