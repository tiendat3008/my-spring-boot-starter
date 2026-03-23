package com.spring.starter.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "FIELD_REQUIRED")
        @Email(message = "INVALID_EMAIL")
        String email
) {
}
