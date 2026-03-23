package com.spring.starter.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "FIELD_REQUIRED")
        String token
) {

}
