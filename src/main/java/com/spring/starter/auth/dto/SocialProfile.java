package com.spring.starter.auth.dto;

public record SocialProfile(
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl
) {

}
