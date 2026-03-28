package com.example.webrtcbackend.transcription.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class TranscribeRequest {

    @NotBlank(message = "Video URL is required")
    @URL(message = "Must be a valid URL")
    private String videoUrl;

    /**
     * Optional: link the transcript to an existing VideoMetadata record.
     */
    private Long videoId;

    public TranscribeRequest() {
    }

    public TranscribeRequest(String videoUrl, Long videoId) {
        this.videoUrl = videoUrl;
        this.videoId = videoId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }
}
