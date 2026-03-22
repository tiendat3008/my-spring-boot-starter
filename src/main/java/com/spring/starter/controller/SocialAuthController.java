package com.spring.starter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.dto.request.SocialLoginRequest;
import com.spring.starter.dto.response.ApiResponse;
import com.spring.starter.dto.response.OAuthStateResponse;
import com.spring.starter.dto.response.SocialLoginResponse;
import com.spring.starter.enums.SocialProvider;
import com.spring.starter.exception.AppException;
import com.spring.starter.exception.ErrorCode;
import com.spring.starter.service.CookieService;
import com.spring.starter.service.SocialAuthService;

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
    ResponseEntity<ApiResponse<SocialLoginResponse>> callback(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletResponse response
    ) {
        SocialProvider socialProvider = parseProvider(provider);
        SocialLoginResponse result = socialAuthService.authenticate(socialProvider, request);

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
