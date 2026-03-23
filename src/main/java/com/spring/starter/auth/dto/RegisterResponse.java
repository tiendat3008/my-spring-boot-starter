package com.spring.starter.auth.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterResponse(
        Long userId,
        String email,
        List<String> roles,
        String firstName,
        String lastName
) {

}
