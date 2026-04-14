package com.example.webrtcbackend.quizresult.dto;

import java.util.List;

/**
 * Aggregated statistics for a user across all or specific assignments.
 */
public class UserStatsResponse {

    private Long userId;
    private String username;
    private long totalAttempts;
    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;
    private List<ProgressPoint> progressHistory;

    // ---- Getters & Setters ----

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public Double getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(Double highestScore) {
        this.highestScore = highestScore;
    }

    public Double getLowestScore() {
        return lowestScore;
    }

    public void setLowestScore(Double lowestScore) {
        this.lowestScore = lowestScore;
    }

    public List<ProgressPoint> getProgressHistory() {
        return progressHistory;
    }

    public void setProgressHistory(List<ProgressPoint> progressHistory) {
        this.progressHistory = progressHistory;
    }

    /**
     * A single data point in the user's progress timeline.
     */
    public static class ProgressPoint {

        private Long resultId;
        private Long assignmentId;
        private String assignmentTitle;
        private Double score;
        private Integer correctCount;
        private Integer totalQuestions;
        private String completedAt;

        public Long getResultId() {
            return resultId;
        }

        public void setResultId(Long resultId) {
            this.resultId = resultId;
        }

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

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Integer getCorrectCount() {
            return correctCount;
        }

        public void setCorrectCount(Integer correctCount) {
            this.correctCount = correctCount;
        }

        public Integer getTotalQuestions() {
            return totalQuestions;
        }

        public void setTotalQuestions(Integer totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }
    }
}
