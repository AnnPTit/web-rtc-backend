package com.example.webrtcbackend.vocabulary.dto;

import java.util.Map;

public class VocabularyStatsResponse {

    private Long userId;
    private long totalWordsLearned;
    private long totalFavorites;
    private long totalNeedReview;
    private long totalWordsStudied;
    private Map<String, Long> topicBreakdown;

    // ---- Getters & Setters ----

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getTotalWordsLearned() {
        return totalWordsLearned;
    }

    public void setTotalWordsLearned(long totalWordsLearned) {
        this.totalWordsLearned = totalWordsLearned;
    }

    public long getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(long totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public long getTotalNeedReview() {
        return totalNeedReview;
    }

    public void setTotalNeedReview(long totalNeedReview) {
        this.totalNeedReview = totalNeedReview;
    }

    public long getTotalWordsStudied() {
        return totalWordsStudied;
    }

    public void setTotalWordsStudied(long totalWordsStudied) {
        this.totalWordsStudied = totalWordsStudied;
    }

    public Map<String, Long> getTopicBreakdown() {
        return topicBreakdown;
    }

    public void setTopicBreakdown(Map<String, Long> topicBreakdown) {
        this.topicBreakdown = topicBreakdown;
    }
}
