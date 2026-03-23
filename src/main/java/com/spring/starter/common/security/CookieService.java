package com.spring.starter.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.spring.starter.common.config.JwtProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CookieService {

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    private final JwtProperties jwtProperties;

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_PATH = "/api/v1/auth";

    public void addRefreshToken(HttpServletResponse response, String refreshToken) {

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(REFRESH_PATH)
                .maxAge(jwtProperties.refreshTokenExpirySeconds())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshToken(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(REFRESH_PATH)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
