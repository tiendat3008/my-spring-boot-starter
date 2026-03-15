package com.spring.starter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.dto.request.AuthRequest;
import com.spring.starter.dto.request.LogoutRequest;
import com.spring.starter.dto.request.RefreshTokenRequest;
import com.spring.starter.dto.request.RegisterRequest;
import com.spring.starter.dto.response.ApiResponse;
import com.spring.starter.dto.response.AuthResponse;
import com.spring.starter.dto.response.RegisterResponse;
import com.spring.starter.service.AuthService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/register")
    ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        var result = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", result));
    }

    @PostMapping("/token")
    ResponseEntity<ApiResponse<AuthResponse>> authenticate(
            @RequestBody AuthRequest request) {
        var result = authService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refreshAccessToken(
            @RequestBody RefreshTokenRequest request) {
        var result = authService.refreshAccessToken(request.token());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<String>> logout(
            @RequestBody LogoutRequest request) {
        authService.logout(request.token());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));   
    }
}
