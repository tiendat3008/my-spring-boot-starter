package com.spring.starter.common.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.spring.starter.auth.entity.User;
import com.spring.starter.common.config.JwtProperties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtTokenProvider {

    JwtProperties jwtProperties;
    JwtEncoder jwtEncoder;
    JwtDecoder jwtDecoder;

    public JwtTokenProvider(
            JwtProperties jwtProperties,
            JwtEncoder jwtEncoder,
            @Qualifier("appJwtDecoder") JwtDecoder jwtDecoder) {
        this.jwtProperties = jwtProperties;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

	public String issueAccessToken(User user) {
        return buildToken(user, jwtProperties.accessTokenExpirySeconds(), "access");
    }

    public String issueRefreshToken(User user) {
        return buildToken(user, jwtProperties.refreshTokenExpirySeconds(), "refresh");
    }

    private String buildToken(User user, Long expirySeconds, String type) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(ur -> ur.getRole().name())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .claim("roles", String.join(" ", roles))
                .claim("type", type)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Jwt extractToken(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException ex) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}
