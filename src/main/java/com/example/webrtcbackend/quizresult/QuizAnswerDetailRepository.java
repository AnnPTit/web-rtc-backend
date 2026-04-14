package com.example.webrtcbackend.quizresult;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerDetailRepository extends JpaRepository<QuizAnswerDetail, Long> {

    List<QuizAnswerDetail> findByQuizResultId(Long quizResultId);

    /**
     * Accuracy statistics per question for a given assignment:
     * returns [questionId, totalAttempts, correctCount].
     */
    @Query("SELECT d.questionId, COUNT(d.id), SUM(CASE WHEN d.isCorrect = true THEN 1 ELSE 0 END) " +
           "FROM QuizAnswerDetail d " +
           "JOIN d.quizResult r " +
           "WHERE r.assignmentId = :assignmentId " +
           "GROUP BY d.questionId")
    List<Object[]> findQuestionAccuracyByAssignment(@Param("assignmentId") Long assignmentId);
}
