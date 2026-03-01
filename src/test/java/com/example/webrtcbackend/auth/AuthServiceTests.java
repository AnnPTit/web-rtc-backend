package com.example.webrtcbackend.auth;

import com.example.webrtcbackend.auth.dto.AuthResponse;
import com.example.webrtcbackend.auth.dto.LoginRequest;
import com.example.webrtcbackend.auth.dto.RegisterRequest;
import com.example.webrtcbackend.auth.service.AuthService;
import com.example.webrtcbackend.config.LiquibaseTestConfig;
import com.example.webrtcbackend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(LiquibaseTestConfig.class)
class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    void registerAndLoginSuccessfully() {
        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setRole(UserRole.STUDENT);

        AuthResponse registerResponse = authService.register(registerRequest);

        assertThat(registerResponse.getToken()).isNotNull();
        assertThat(registerResponse.getUsername()).isEqualTo("testuser");
        assertThat(registerResponse.getRole()).isEqualTo(UserRole.STUDENT);

        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        AuthResponse loginResponse = authService.login(loginRequest);

        assertThat(loginResponse.getToken()).isNotNull();
        assertThat(loginResponse.getUserId()).isEqualTo(registerResponse.getUserId());
    }
}

