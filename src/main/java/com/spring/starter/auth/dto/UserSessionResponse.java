package com.spring.starter.auth.dto;

import java.time.Instant;

public record UserSessionResponse(String sessionId, String ipAddress, String userAgent, Instant loginTime) {}
