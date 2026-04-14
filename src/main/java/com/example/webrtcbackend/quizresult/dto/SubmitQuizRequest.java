package com.example.webrtcbackend.quizresult.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for submitting quiz results.
 */
public class SubmitQuizRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "assignmentId is required")
    private Long assignmentId;

    @NotEmpty(message = "answers list must not be empty")
    @Valid
    private List<AnswerItem> answers;

    /**
     * Time taken to complete the quiz, in seconds. Optional.
     */
    private Integer durationSeconds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public List<AnswerItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerItem> answers) {
        this.answers = answers;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * Individual answer submission for one question.
     */
    public static class AnswerItem {

        @NotNull(message = "questionId is required")
        private Long questionId;

        /**
         * The option key selected by the user (A, B, C, D). Null if skipped.
         */
        private String selectedAnswer;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public String getSelectedAnswer() {
            return selectedAnswer;
        }

        public void setSelectedAnswer(String selectedAnswer) {
            this.selectedAnswer = selectedAnswer;
        }
    }
}
