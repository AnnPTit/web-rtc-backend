package com.example.webrtcbackend.vocabulary;

import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.vocabulary.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private static final Logger log = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    // ========================================================================
    // 1. Generate vocabulary
    // ========================================================================

    /**
     * POST /api/vocabulary/generate
     * Generate vocabulary words by topic, level, and quantity.
     * Uses DB-first strategy, falls back to Gemini AI if insufficient.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<VocabularyWordResponse>>> generateVocabulary(
            @Valid @RequestBody GenerateVocabularyRequest request) {
        log.info("POST /api/vocabulary/generate – topic={}, level={}, quantity={}, userId={}",
                request.getTopic(), request.getLevel(), request.getQuantity(), request.getUserId());
        List<VocabularyWordResponse> words = vocabularyService.generateVocabulary(request);
        return ResponseEntity.ok(ApiResponse.ok("Vocabulary generated successfully", words));
    }

    // ========================================================================
    // 2. Get available topics
    // ========================================================================

    /**
     * GET /api/vocabulary/topics
     * Get list of available vocabulary topics.
     */
    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<String>>> getTopics() {
        log.info("GET /api/vocabulary/topics");
        List<String> topics = vocabularyService.getAvailableTopics();
        return ResponseEntity.ok(ApiResponse.ok(topics));
    }

    // ========================================================================
    // 3. Get vocabulary by topic
    // ========================================================================

    /**
     * GET /api/vocabulary/by-topic?topic=X&level=Y&userId=Z
     * Get all words for a topic+level with user progress embedded.
     */
    @GetMapping("/by-topic")
    public ResponseEntity<ApiResponse<List<VocabularyWordResponse>>> getByTopic(
            @RequestParam String topic,
            @RequestParam String level,
            @RequestParam(required = false) Long userId) {
        log.info("GET /api/vocabulary/by-topic – topic={}, level={}, userId={}", topic, level, userId);
        List<VocabularyWordResponse> words = vocabularyService.getVocabularyByTopic(topic, level, userId);
        return ResponseEntity.ok(ApiResponse.ok(words));
    }

    // ========================================================================
    // 4. Update learning progress
    // ========================================================================

    /**
     * PUT /api/vocabulary/progress
     * Update learning progress for a vocabulary word (learned, favorite, review).
     */
    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<VocabularyWordResponse>> updateProgress(
            @Valid @RequestBody UpdateProgressRequest request) {
        log.info("PUT /api/vocabulary/progress – userId={}, vocabularyId={}",
                request.getUserId(), request.getVocabularyId());
        VocabularyWordResponse response = vocabularyService.updateProgress(request);
        return ResponseEntity.ok(ApiResponse.ok("Progress updated successfully", response));
    }

    // ========================================================================
    // 5. Get user progress
    // ========================================================================

    /**
     * GET /api/vocabulary/progress/{userId}
     * Get all vocabulary progress for a user.
     */
    @GetMapping("/progress/{userId}")
    public ResponseEntity<ApiResponse<List<VocabularyWordResponse>>> getUserProgress(
            @PathVariable Long userId) {
        log.info("GET /api/vocabulary/progress/{}", userId);
        List<VocabularyWordResponse> progress = vocabularyService.getUserProgress(userId);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    // ========================================================================
    // 6. Get user vocabulary stats
    // ========================================================================

    /**
     * GET /api/vocabulary/stats/{userId}
     * Get vocabulary learning statistics for a user.
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<ApiResponse<VocabularyStatsResponse>> getUserStats(
            @PathVariable Long userId) {
        log.info("GET /api/vocabulary/stats/{}", userId);
        VocabularyStatsResponse stats = vocabularyService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ========================================================================
    // 7. Get favorite words
    // ========================================================================

    /**
     * GET /api/vocabulary/favorites/{userId}
     * Get all words marked as favorite by the user.
     */
    @GetMapping("/favorites/{userId}")
    public ResponseEntity<ApiResponse<List<VocabularyWordResponse>>> getFavorites(
            @PathVariable Long userId) {
        log.info("GET /api/vocabulary/favorites/{}", userId);
        List<VocabularyWordResponse> favorites = vocabularyService.getFavoriteWords(userId);
        return ResponseEntity.ok(ApiResponse.ok(favorites));
    }

    // ========================================================================
    // 8. Get words needing review
    // ========================================================================

    /**
     * GET /api/vocabulary/review/{userId}
     * Get all words marked for review by the user.
     */
    @GetMapping("/review/{userId}")
    public ResponseEntity<ApiResponse<List<VocabularyWordResponse>>> getReviewWords(
            @PathVariable Long userId) {
        log.info("GET /api/vocabulary/review/{}", userId);
        List<VocabularyWordResponse> reviewWords = vocabularyService.getReviewWords(userId);
        return ResponseEntity.ok(ApiResponse.ok(reviewWords));
    }
}
