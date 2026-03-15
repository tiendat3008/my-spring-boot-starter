package com.spring.starter.exception;

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
    INVALID_TOKEN("INVALID_TOKEN", "Invalid JWT token", HttpStatus.UNAUTHORIZED)
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
