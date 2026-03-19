package com.spring.starter.dto.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, null, Instant.now());
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>("SUCCESS", null, null, null, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, null, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message, List<FieldErrorResponse> errors) {
        return new ApiResponse<>(code, message, null, errors, Instant.now());
    }
}
