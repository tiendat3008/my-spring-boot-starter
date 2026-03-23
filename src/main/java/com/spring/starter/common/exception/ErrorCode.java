package com.spring.starter.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("UNCATEGORIZED_EXCEPTION", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed",HttpStatus.BAD_REQUEST),
    FIELD_REQUIRED("FIELD_REQUIRED", "{field} is required", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL("INVALID_EMAIL", "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_SIZE("INVALID_PASSWORD_SIZE", "{field} must be {min}-{max} characters", HttpStatus.BAD_REQUEST),

    USER_EXISTED("USER_EXISTED", "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED("USER_NOT_EXISTED", "User not existed", HttpStatus.NOT_FOUND),
    
    UNAUTHENTICATED("UNAUTHENTICATED", "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "You do not have permission", HttpStatus.FORBIDDEN),
    TOKEN_REVOKED("TOKEN_REVOKED", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token has expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("INVALID_TOKEN", "Invalid JWT token", HttpStatus.UNAUTHORIZED),
    MISSING_REFRESH_TOKEN("MISSING_REFRESH_TOKEN", "Refresh token is missing", HttpStatus.BAD_REQUEST),

    OAUTH_PROVIDER_UNSUPPORTED("OAUTH_PROVIDER_UNSUPPORTED", "Unsupported social provider", HttpStatus.BAD_REQUEST),
    OAUTH_EXCHANGE_FAILED("OAUTH_EXCHANGE_FAILED", "Failed to exchange OAuth authorization code", HttpStatus.BAD_GATEWAY),
    OAUTH_STATE_INVALID("OAUTH_STATE_INVALID", "Invalid or expired OAuth state", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "Email not verified by provider", HttpStatus.BAD_REQUEST);
    ;

    private final String code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(String code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }  
}
