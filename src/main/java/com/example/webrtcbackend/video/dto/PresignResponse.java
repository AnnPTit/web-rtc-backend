package com.example.webrtcbackend.video.dto;

import java.time.Instant;

public class PresignResponse {
    private String uploadUrl;
    private String objectKey;
    private Instant expiresAt;

    public PresignResponse() {
    }

    public PresignResponse(String uploadUrl, String objectKey, Instant expiresAt) {
        this.uploadUrl = uploadUrl;
        this.objectKey = objectKey;
        this.expiresAt = expiresAt;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
