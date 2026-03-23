package com.spring.starter.auth.enums;

public enum SocialProvider {
    GOOGLE,
    FACEBOOK,
    GITHUB;

    public static SocialProvider from(String value) {
        return SocialProvider.valueOf(value.toUpperCase());
    }
}
