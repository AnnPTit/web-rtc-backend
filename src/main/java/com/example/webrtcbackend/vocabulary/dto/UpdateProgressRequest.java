package com.example.webrtcbackend.vocabulary.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateProgressRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Vocabulary ID is required")
    private Long vocabularyId;

    private Boolean learnedFlag;
    private Boolean favoriteFlag;
    private Boolean needReviewFlag;

    // ---- Getters & Setters ----

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(Long vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

    public Boolean getLearnedFlag() {
        return learnedFlag;
    }

    public void setLearnedFlag(Boolean learnedFlag) {
        this.learnedFlag = learnedFlag;
    }

    public Boolean getFavoriteFlag() {
        return favoriteFlag;
    }

    public void setFavoriteFlag(Boolean favoriteFlag) {
        this.favoriteFlag = favoriteFlag;
    }

    public Boolean getNeedReviewFlag() {
        return needReviewFlag;
    }

    public void setNeedReviewFlag(Boolean needReviewFlag) {
        this.needReviewFlag = needReviewFlag;
    }
}
