package com.spring.starter.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.auth.dto.AuthRequest;
import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.ChangePasswordRequest;
import com.spring.starter.auth.dto.ForgotPasswordRequest;
import com.spring.starter.auth.dto.LogoutRequest;
import com.spring.starter.auth.dto.ResendOtpRequest;
import com.spring.starter.auth.dto.RefreshTokenRequest;
import com.spring.starter.auth.dto.RegisterRequest;
import com.spring.starter.auth.dto.RegisterResponse;
import com.spring.starter.auth.dto.ResetPasswordRequest;
import com.spring.starter.auth.dto.UserSessionResponse;
import com.spring.starter.auth.dto.VerifyEmailRequest;
import com.spring.starter.auth.service.AuthRecoveryService;
import com.spring.starter.auth.service.AuthService;
import com.spring.starter.common.dto.ApiResponse;
import com.spring.starter.common.security.CookieService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;
    AuthRecoveryService authRecoveryService;
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

    @PostMapping("/change-password")
    ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.changePassword(authentication.getName(), request, refreshToken);

        if (refreshToken != null && !refreshToken.isBlank()) {
            cookieService.clearRefreshToken(response);
        }

        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/forgot-password")
    ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authRecoveryService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, an OTP has been sent", null));
    }

    @PostMapping("/reset-password")
    ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authRecoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/resend-otp")
    ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authRecoveryService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a new OTP has been sent", null));
    }

    @PostMapping("/verify-email")
    ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authRecoveryService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/sessions")
    ResponseEntity<ApiResponse<List<UserSessionResponse>>> sessions(Authentication authentication) {
        var result = authService.listSessions(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/sessions/{sessionId}")
    ResponseEntity<ApiResponse<Void>> revokeSession(
            Authentication authentication,
            @PathVariable String sessionId
    ) {
        authService.revokeSession(authentication.getName(), sessionId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
