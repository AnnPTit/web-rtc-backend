package com.example.webrtcbackend.transcription.dto;

import java.time.Instant;
import java.util.List;

public class AssignmentResponse {

    private Long id;
    private Long lessonId;
    private String title;
    private String description;
    private String statusProgress;
    private String errorMessage;
    private Instant createdAt;
    private List<QuestionDTO> questions;

    public AssignmentResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatusProgress() {
        return statusProgress;
    }

    public void setStatusProgress(String statusProgress) {
        this.statusProgress = statusProgress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<QuestionDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDTO> questions) {
        this.questions = questions;
    }
}
