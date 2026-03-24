package com.spring.starter.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SocialLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        UserInfo user) {
    public record UserInfo(Long userId, String email, String displayName, String avatarUrl, boolean newUser) {}
}
