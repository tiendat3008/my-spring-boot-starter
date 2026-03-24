package com.spring.starter.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.OAuthStateResponse;
import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.auth.service.SocialAuthService;
import com.spring.starter.common.dto.ApiResponse;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.common.security.CookieService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialAuthController {

    SocialAuthService socialAuthService;
    CookieService cookieService;

    @GetMapping("/state/{provider}")
    ResponseEntity<ApiResponse<OAuthStateResponse>> state(@PathVariable String provider) {
        SocialProvider socialProvider = parseProvider(provider);
        String state = socialAuthService.issueState(socialProvider);

        return ResponseEntity.ok(ApiResponse.success(new OAuthStateResponse(state)));
    }

    @PostMapping("/callback/{provider}")
    ResponseEntity<ApiResponse<AuthResponse>> callback(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        SocialProvider socialProvider = parseProvider(provider);
        AuthResponse result = socialAuthService.authenticate(socialProvider, request, httpRequest);

        cookieService.addRefreshToken(response, result.refreshToken());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.from(provider);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.OAUTH_PROVIDER_UNSUPPORTED);
        }
    }
}
