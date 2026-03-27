package com.example.webrtcbackend.auth.controller;

import com.example.webrtcbackend.auth.dto.AuthResponse;
import com.example.webrtcbackend.auth.dto.LoginRequest;
import com.example.webrtcbackend.auth.dto.RegisterRequest;
import com.example.webrtcbackend.auth.service.AuthService;
import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Đăng ký tài khoản mới.
     *
     * Request body:
     * {
     *   "username": "john_doe",
     *   "password": "secret123",
     *   "confirmPassword": "secret123",
     *   "fullName": "John Doe",
     *   "email": "john@example.com",
     *   "role": "STUDENT"   // tùy chọn, mặc định STUDENT
     * }
     *
     * Response 201: { success, message, data: { token, userId, username, fullName, email, role } }
     * Response 400: validation errors
     * Response 409: username/email đã tồn tại
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse data = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký tài khoản thành công", data));
    }

    /**
     * POST /api/auth/login
     * Đăng nhập và nhận JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * Lấy thông tin người dùng hiện tại (yêu cầu JWT).
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(new AuthResponse(
                null,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        ));
    }
}
