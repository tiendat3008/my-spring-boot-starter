package com.spring.starter.common.dto;

public record FieldErrorResponse(
        String field,
        String message
) {

}
