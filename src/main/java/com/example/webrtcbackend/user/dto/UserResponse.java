package com.example.webrtcbackend.user.dto;

import com.example.webrtcbackend.user.UserRole;
import java.time.Instant;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private Instant createdAt;

    public UserResponse(Long id, String username, String fullName, String email, UserRole role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

