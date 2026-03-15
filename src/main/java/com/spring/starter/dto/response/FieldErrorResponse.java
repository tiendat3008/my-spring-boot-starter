package com.spring.starter.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) {

}
