package com.spring.starter.dto.request;

public record AuthRequest(
        String email,
        String password
) {

}
