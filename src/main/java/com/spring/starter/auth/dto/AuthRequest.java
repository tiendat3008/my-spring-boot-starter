package com.spring.starter.auth.dto;

public record AuthRequest(
        String email,
        String password
) {

}
