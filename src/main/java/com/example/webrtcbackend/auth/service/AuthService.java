package com.example.webrtcbackend.auth.service;

import com.example.webrtcbackend.auth.dto.AuthResponse;
import com.example.webrtcbackend.auth.dto.LoginRequest;
import com.example.webrtcbackend.auth.dto.RegisterRequest;
import com.example.webrtcbackend.auth.repository.AuthRepository;
import com.example.webrtcbackend.common.ConflictException;
import com.example.webrtcbackend.user.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // 2. Kiểm tra username đã tồn tại
        if (authRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' đã được sử dụng");
        }

        // 3. Kiểm tra email đã tồn tại
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' đã được đăng ký");
        }

        // 4. Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setRole(request.getRole());
        user.setCreatedAt(Instant.now());

        User savedUser;
        try {
            savedUser = authRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: username hoặc email bị trùng giữa check và insert
            throw new ConflictException("Username hoặc email đã tồn tại");
        }

        // 5. Tạo JWT token
        String token = jwtService.generateToken(
                savedUser.getUsername(),
                savedUser.getRole().name(),
                savedUser.getId()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = authRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Username hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Username hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return authRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
