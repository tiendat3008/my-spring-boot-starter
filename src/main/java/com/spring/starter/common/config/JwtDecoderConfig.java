package com.spring.starter.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

@Configuration
public class JwtDecoderConfig {

    @Bean("googleJwtDecoder")
    JwtDecoder googleJwtDecoder(OAuth2Properties oAuth2Properties) {

        String clientId = oAuth2Properties.getProvider("google").clientId();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();

        // Default validators: expiration, not before, signature, issuer
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators
                .createDefaultWithIssuer("https://accounts.google.com");

        // Validator to check audience claim contains our client ID
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audience = token.getAudience();
            if (audience != null && audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience claim", JwtClaimNames.AUD));
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator)
        );

        return decoder;
    }
}
