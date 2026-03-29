package com.example.webrtcbackend.transcription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "transcripts")
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The AssemblyAI transcript ID (e.g. "6t7w8kxyz9").
     */
    @Column(name = "assembly_transcript_id", nullable = false, length = 100)
    private String assemblyTranscriptId;

    @Column(name = "video_url", nullable = false, length = 2000)
    private String videoUrl;

    /**
     * Optional FK to the videos table.
     */
    @Column(name = "video_id")
    private String videoId;

    /**
     * queued | processing | completed | error
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "transcript_text", columnDefinition = "LONGTEXT")
    private String transcriptText;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "assignment_id")
    private Long assignmentId;

    public Transcript() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssemblyTranscriptId() {
        return assemblyTranscriptId;
    }

    public void setAssemblyTranscriptId(String assemblyTranscriptId) {
        this.assemblyTranscriptId = assemblyTranscriptId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }
}
