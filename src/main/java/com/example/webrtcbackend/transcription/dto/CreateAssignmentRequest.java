package com.example.webrtcbackend.transcription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateAssignmentRequest {

    @NotNull(message = "Lesson ID is required")
    private Long lessonId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    public CreateAssignmentRequest() {
    }

    public CreateAssignmentRequest(Long lessonId, String title, String description) {
        this.lessonId = lessonId;
        this.title = title;
        this.description = description;
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
}
