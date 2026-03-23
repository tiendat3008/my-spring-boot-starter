package com.spring.starter.auth.dto;

import java.util.List;

public record RegisterResponse(
        Long userId,
        String email,
        List<String> roles
) {

}
