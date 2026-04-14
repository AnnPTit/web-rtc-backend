package com.example.webrtcbackend.quizresult;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    /**
     * Find all results for a user, ordered by most recent first.
     */
    List<QuizResult> findByUserIdOrderByCompletedAtDesc(Long userId);

    /**
     * Find all results for a specific assignment.
     */
    List<QuizResult> findByAssignmentIdOrderByCompletedAtDesc(Long assignmentId);

    /**
     * Find all results for a user on a specific assignment (history).
     */
    List<QuizResult> findByUserIdAndAssignmentIdOrderByCompletedAtDesc(Long userId, Long assignmentId);

    /**
     * Find the most recent result for a user on a given assignment.
     */
    Optional<QuizResult> findFirstByUserIdAndAssignmentIdOrderByCompletedAtDesc(Long userId, Long assignmentId);

    /**
     * Find the most recent result for a user (across all assignments).
     */
    Optional<QuizResult> findFirstByUserIdOrderByCompletedAtDesc(Long userId);

    /**
     * Count total attempts by a user.
     */
    long countByUserId(Long userId);

    /**
     * Count total attempts on an assignment.
     */
    long countByAssignmentId(Long assignmentId);

    /**
     * Average score for a user.
     */
    @Query("SELECT AVG(q.score) FROM QuizResult q WHERE q.userId = :userId")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    /**
     * Max score for a user.
     */
    @Query("SELECT MAX(q.score) FROM QuizResult q WHERE q.userId = :userId")
    Double findMaxScoreByUserId(@Param("userId") Long userId);

    /**
     * Min score for a user.
     */
    @Query("SELECT MIN(q.score) FROM QuizResult q WHERE q.userId = :userId")
    Double findMinScoreByUserId(@Param("userId") Long userId);

    /**
     * Average score for a user on a specific assignment.
     */
    @Query("SELECT AVG(q.score) FROM QuizResult q WHERE q.userId = :userId AND q.assignmentId = :assignmentId")
    Double findAverageScoreByUserIdAndAssignmentId(@Param("userId") Long userId, @Param("assignmentId") Long assignmentId);

    /**
     * Max score for a user on a specific assignment.
     */
    @Query("SELECT MAX(q.score) FROM QuizResult q WHERE q.userId = :userId AND q.assignmentId = :assignmentId")
    Double findMaxScoreByUserIdAndAssignmentId(@Param("userId") Long userId, @Param("assignmentId") Long assignmentId);

    /**
     * Results for a user within a date range, for progress tracking.
     */
    List<QuizResult> findByUserIdAndCompletedAtBetweenOrderByCompletedAtAsc(Long userId, Instant from, Instant to);

    /**
     * Average score per assignment for ranking/difficulty analysis.
     */
    @Query("SELECT q.assignmentId, AVG(q.score), COUNT(q.id) FROM QuizResult q " +
           "WHERE q.assignmentId = :assignmentId GROUP BY q.assignmentId")
    List<Object[]> findAssignmentStats(@Param("assignmentId") Long assignmentId);

    /**
     * Top N recent results for a user.
     */
    List<QuizResult> findByUserIdOrderByCompletedAtDesc(Long userId, Pageable pageable);
}
