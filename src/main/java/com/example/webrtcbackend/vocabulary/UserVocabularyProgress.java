package com.example.webrtcbackend.vocabulary;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_vocabulary_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_vocab",
                columnNames = {"user_id", "vocabulary_id"}))
public class UserVocabularyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vocabulary_id", nullable = false)
    private Long vocabularyId;

    @Column(name = "learned_flag", nullable = false)
    private Boolean learnedFlag = false;

    @Column(name = "learned_at")
    private Instant learnedAt;

    @Column(name = "favorite_flag", nullable = false)
    private Boolean favoriteFlag = false;

    @Column(name = "need_review_flag", nullable = false)
    private Boolean needReviewFlag = false;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount = 0;

    @Column(name = "last_review_at")
    private Instant lastReviewAt;

    // ---- Getters & Setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Instant getLearnedAt() {
        return learnedAt;
    }

    public void setLearnedAt(Instant learnedAt) {
        this.learnedAt = learnedAt;
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

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Instant getLastReviewAt() {
        return lastReviewAt;
    }

    public void setLastReviewAt(Instant lastReviewAt) {
        this.lastReviewAt = lastReviewAt;
    }
}
