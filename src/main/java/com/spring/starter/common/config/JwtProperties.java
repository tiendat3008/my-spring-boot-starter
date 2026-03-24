package com.spring.starter.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String publicKey,
        @NotBlank String privateKey,
        @Positive long accessTokenExpirySeconds,
        @Positive long refreshTokenExpirySeconds,
        @NotBlank String issuer) {}
