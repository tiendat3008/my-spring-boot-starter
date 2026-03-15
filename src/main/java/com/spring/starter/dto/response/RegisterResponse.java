package com.spring.starter.dto.response;

import java.util.List;

public record RegisterResponse(
        Long userId,
        String email,
        List<String> roles
) {

}
