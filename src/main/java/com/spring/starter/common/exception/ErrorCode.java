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
    INVALID_CURRENT_PASSWORD("INVALID_CURRENT_PASSWORD", "Current password is incorrect", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_CHANGED("PASSWORD_NOT_CHANGED", "New password must be different from current password", HttpStatus.BAD_REQUEST),

    OAUTH_PROVIDER_UNSUPPORTED("OAUTH_PROVIDER_UNSUPPORTED", "Unsupported social provider", HttpStatus.BAD_REQUEST),
    OAUTH_EXCHANGE_FAILED("OAUTH_EXCHANGE_FAILED", "Failed to exchange OAuth authorization code", HttpStatus.BAD_GATEWAY),
    OAUTH_STATE_INVALID("OAUTH_STATE_INVALID", "Invalid or expired OAuth state", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "Email not verified by provider", HttpStatus.BAD_REQUEST),

    INVALID_NAME_SIZE("INVALID_NAME_SIZE", "{field} must be at most {max} characters", HttpStatus.BAD_REQUEST),
    INVALID_BIO_SIZE("INVALID_BIO_SIZE", "{field} must be at most {max} characters", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_URL_SIZE("INVALID_AVATAR_URL_SIZE", "{field} must be at most {max} characters", HttpStatus.BAD_REQUEST),
    INVALID_PREFERRED_LANGUAGE_SIZE("INVALID_PREFERRED_LANGUAGE_SIZE", "{field} must be at most {max} characters", HttpStatus.BAD_REQUEST),
    INVALID_PREFERRED_CURRENCY_SIZE("INVALID_PREFERRED_CURRENCY_SIZE", "{field} must be at most {max} characters", HttpStatus.BAD_REQUEST),

    INVALID_OTP_SIZE("INVALID_OTP_SIZE", "{field} must be {min} characters", HttpStatus.BAD_REQUEST),
    OTP_INVALID_OR_EXPIRED("OTP_INVALID_OR_EXPIRED", "OTP is invalid or expired", HttpStatus.BAD_REQUEST),
    OTP_PURPOSE_UNSUPPORTED("OTP_PURPOSE_UNSUPPORTED", "OTP purpose is not supported", HttpStatus.BAD_REQUEST),

    SOCIAL_ACCOUNT_ALREADY_LINKED("SOCIAL_ACCOUNT_ALREADY_LINKED", "Social account already linked for this provider", HttpStatus.BAD_REQUEST),
    SOCIAL_ACCOUNT_ALREADY_USED("SOCIAL_ACCOUNT_ALREADY_USED", "Social account is linked with another user", HttpStatus.BAD_REQUEST),
    SOCIAL_ACCOUNT_NOT_FOUND("SOCIAL_ACCOUNT_NOT_FOUND", "Social account not found", HttpStatus.NOT_FOUND),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND);
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
