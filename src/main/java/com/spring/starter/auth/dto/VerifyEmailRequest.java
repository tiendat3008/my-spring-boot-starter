package com.spring.starter.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotBlank(message = "FIELD_REQUIRED")
        @Email(message = "INVALID_EMAIL")
        String email,

        @NotBlank(message = "FIELD_REQUIRED")
        @Size(min = 6, max = 6, message = "INVALID_OTP_SIZE")
        String otp
) {
}
