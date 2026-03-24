package com.spring.starter.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 100, message = "INVALID_NAME_SIZE") String firstName,
        @Size(max = 100, message = "INVALID_NAME_SIZE") String lastName,
        @Size(max = 1000, message = "INVALID_BIO_SIZE") String bio,
        @Size(max = 10, message = "INVALID_PREFERRED_LANGUAGE_SIZE") String preferredLanguage,
        @Size(max = 10, message = "INVALID_PREFERRED_CURRENCY_SIZE") String preferredCurrency) {}
