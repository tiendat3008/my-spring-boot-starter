package com.spring.starter.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.auth.dto.AuthRequest;
import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.LogoutRequest;
import com.spring.starter.auth.dto.RefreshTokenRequest;
import com.spring.starter.auth.dto.RegisterRequest;
import com.spring.starter.auth.dto.RegisterResponse;
import com.spring.starter.auth.service.AuthService;
import com.spring.starter.common.dto.ApiResponse;
import com.spring.starter.common.security.CookieService;

import jakarta.servlet.http.HttpServletResponse;
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
    CookieService cookieService;

    @PostMapping("/register")
    ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        var result = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", result));
    }

    @PostMapping("/token")
    ResponseEntity<ApiResponse<AuthResponse>> authenticate(
            @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        var result = authService.authenticate(request);
        cookieService.addRefreshToken(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        var result = authService.refreshAccessToken(refreshToken);
        cookieService.addRefreshToken(response, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        cookieService.clearRefreshToken(response);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/refresh/mobile")
    ResponseEntity<ApiResponse<AuthResponse>> refreshMobile(
            @RequestBody RefreshTokenRequest request
    ) {
        var result = authService.refreshAccessToken(request.token());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout/mobile")
    ResponseEntity<ApiResponse<Void>> logoutMobile(
            @RequestBody LogoutRequest request
    ) {
        authService.logout(request.token());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
