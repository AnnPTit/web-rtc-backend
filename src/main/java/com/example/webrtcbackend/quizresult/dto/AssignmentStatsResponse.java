package com.example.webrtcbackend.quizresult.dto;

import java.util.List;

/**
 * Statistics for an assignment with per-question accuracy analysis.
 */
public class AssignmentStatsResponse {

    private Long assignmentId;
    private String assignmentTitle;
    private long totalAttempts;
    private Double averageScore;
    private List<QuestionAccuracy> questionAccuracies;

    // ---- Getters & Setters ----

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(long totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public List<QuestionAccuracy> getQuestionAccuracies() {
        return questionAccuracies;
    }

    public void setQuestionAccuracies(List<QuestionAccuracy> questionAccuracies) {
        this.questionAccuracies = questionAccuracies;
    }

    /**
     * Per-question accuracy data for difficulty analysis.
     */
    public static class QuestionAccuracy {

        private Long questionId;
        private String questionText;
        private long totalAttempts;
        private long correctCount;
        private long wrongCount;
        private Double accuracyRate;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public long getTotalAttempts() {
            return totalAttempts;
        }

        public void setTotalAttempts(long totalAttempts) {
            this.totalAttempts = totalAttempts;
        }

        public long getCorrectCount() {
            return correctCount;
        }

        public void setCorrectCount(long correctCount) {
            this.correctCount = correctCount;
        }

        public long getWrongCount() {
            return wrongCount;
        }

        public void setWrongCount(long wrongCount) {
            this.wrongCount = wrongCount;
        }

        public Double getAccuracyRate() {
            return accuracyRate;
        }

        public void setAccuracyRate(Double accuracyRate) {
            this.accuracyRate = accuracyRate;
        }
    }
}
