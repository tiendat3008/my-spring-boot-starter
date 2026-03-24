package com.spring.starter.common.util;

import java.util.Map;

import org.springframework.validation.FieldError;

import com.spring.starter.common.exception.ErrorCode;

import jakarta.validation.ConstraintViolation;

public class ValidationUtil {

    public static String buildMessage(FieldError fieldError) {

        String enumKey = fieldError.getDefaultMessage();

        ErrorCode errorCode;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            errorCode = ErrorCode.VALIDATION_ERROR;
        }

        String message = errorCode.getMessage();

        message = message.replace("{field}", fieldError.getField());

        Map<String, Object> attributes = fieldError
                .unwrap(ConstraintViolation.class)
                .getConstraintDescriptor()
                .getAttributes();

        for (var entry : attributes.entrySet()) {

            message = message.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        return message;
    }
}
