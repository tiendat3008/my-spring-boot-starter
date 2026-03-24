package com.spring.starter.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.auth.dto.LinkedSocialAccountResponse;
import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.auth.service.SocialAuthService;
import com.spring.starter.common.dto.ApiResponse;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/auth/social")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialAccountController {

    SocialAuthService socialAuthService;

    @GetMapping("/accounts")
    ResponseEntity<ApiResponse<List<LinkedSocialAccountResponse>>> listLinkedAccounts(Authentication authentication) {
        var result = socialAuthService.listLinkedAccounts(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/link/{provider}")
    ResponseEntity<ApiResponse<Void>> linkAccount(
            Authentication authentication,
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        socialAuthService.linkAccount(authentication.getName(), parseProvider(provider), request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @DeleteMapping("/unlink/{provider}")
    ResponseEntity<ApiResponse<Void>> unlinkAccount(Authentication authentication, @PathVariable String provider) {
        socialAuthService.unlinkAccount(authentication.getName(), parseProvider(provider));
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.from(provider);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.OAUTH_PROVIDER_UNSUPPORTED);
        }
    }
}
