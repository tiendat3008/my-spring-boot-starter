package com.spring.starter.service.social;

public record SocialProfile(
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl
) {

}
