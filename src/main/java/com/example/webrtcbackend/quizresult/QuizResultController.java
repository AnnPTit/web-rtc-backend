package com.example.webrtcbackend.quizresult;

import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.quizresult.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/quiz-results")
public class QuizResultController {

    private static final Logger log = LoggerFactory.getLogger(QuizResultController.class);

    private final QuizResultService quizResultService;

    public QuizResultController(QuizResultService quizResultService) {
        this.quizResultService = quizResultService;
    }

    // ========================================================================
    // 1. Submit a quiz result
    // ========================================================================

    /**
     * POST /api/quiz-results
     * Submit a completed quiz with answers for grading and storage.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuiz(
            @Valid @RequestBody SubmitQuizRequest request) {
        log.info("POST /api/quiz-results – userId={}, assignmentId={}",
                request.getUserId(), request.getAssignmentId());
        QuizResultResponse response = quizResultService.submitQuiz(request);
        return ResponseEntity.ok(ApiResponse.ok("Quiz submitted and graded successfully", response));
    }

    // ========================================================================
    // 2. Get a quiz result by ID
    // ========================================================================

    /**
     * GET /api/quiz-results/{id}
     * Get detailed result of a single quiz attempt.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getResultById(@PathVariable Long id) {
        log.info("GET /api/quiz-results/{}", id);
        QuizResultResponse response = quizResultService.getResultById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================================================
    // 3. Get quiz history for a user on a specific assignment
    // ========================================================================

    /**
     * GET /api/quiz-results/history?userId=X&assignmentId=Y
     * Get all attempts of a user for a specific assignment.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<QuizResultResponse>>> getHistory(
            @RequestParam Long userId,
            @RequestParam Long assignmentId) {
        log.info("GET /api/quiz-results/history – userId={}, assignmentId={}", userId, assignmentId);
        List<QuizResultResponse> responses = quizResultService.getHistory(userId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    // ========================================================================
    // 4. Get latest result for a user (across all assignments)
    // ========================================================================

    /**
     * GET /api/quiz-results/latest?userId=X
     * Get the most recent quiz result for a user.
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getLatestResult(
            @RequestParam Long userId) {
        log.info("GET /api/quiz-results/latest – userId={}", userId);
        QuizResultResponse response = quizResultService.getLatestResult(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================================================
    // 5. Get latest result for a user on a specific assignment
    // ========================================================================

    /**
     * GET /api/quiz-results/latest-for-assignment?userId=X&assignmentId=Y
     * Get the most recent result for a user on a specific assignment.
     */
    @GetMapping("/latest-for-assignment")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getLatestForAssignment(
            @RequestParam Long userId,
            @RequestParam Long assignmentId) {
        log.info("GET /api/quiz-results/latest-for-assignment – userId={}, assignmentId={}", userId, assignmentId);
        QuizResultResponse response = quizResultService.getLatestResultForAssignment(userId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================================================
    // 6. User statistics (overall)
    // ========================================================================

    /**
     * GET /api/quiz-results/stats/user/{userId}
     * Get aggregated statistics for a user: total attempts, avg/max/min scores, progress history.
     */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(@PathVariable Long userId) {
        log.info("GET /api/quiz-results/stats/user/{}", userId);
        UserStatsResponse response = quizResultService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================================================
    // 7. Assignment statistics (with per-question accuracy)
    // ========================================================================

    /**
     * GET /api/quiz-results/stats/assignment/{assignmentId}
     * Get statistics for an assignment: total attempts, avg score, per-question accuracy.
     */
    @GetMapping("/stats/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentStatsResponse>> getAssignmentStats(
            @PathVariable Long assignmentId) {
        log.info("GET /api/quiz-results/stats/assignment/{}", assignmentId);
        AssignmentStatsResponse response = quizResultService.getAssignmentStats(assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================================================
    // 8. User progress over a date range (for charts)
    // ========================================================================

    /**
     * GET /api/quiz-results/progress?userId=X&from=2026-01-01&to=2026-04-14
     * Get quiz results within a date range for progress chart rendering.
     */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<QuizResultResponse>>> getUserProgress(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("GET /api/quiz-results/progress – userId={}, from={}, to={}", userId, from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<QuizResultResponse> responses = quizResultService.getUserProgressByDateRange(userId, fromInstant, toInstant);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    // ========================================================================
    // 9. Get all results for an assignment
    // ========================================================================

    /**
     * GET /api/quiz-results/by-assignment/{assignmentId}
     * Get all quiz attempts for a specific assignment (across all users).
     */
    @GetMapping("/by-assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<List<QuizResultResponse>>> getResultsByAssignment(
            @PathVariable Long assignmentId) {
        log.info("GET /api/quiz-results/by-assignment/{}", assignmentId);
        List<QuizResultResponse> responses = quizResultService.getResultsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
