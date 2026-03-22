package com.spring.starter.enums;

public enum SocialProvider {
    GOOGLE,
    FACEBOOK,
    GITHUB;

    public static SocialProvider from(String value) {
        return SocialProvider.valueOf(value.toUpperCase());
    }
}
