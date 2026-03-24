package com.spring.starter.auth.dto;

import java.time.Instant;

public record LinkedSocialAccountResponse(
        String provider,
        String providerEmail,
        String providerDisplayName,
        String providerAvatarUrl,
        Instant linkedAt) {}
