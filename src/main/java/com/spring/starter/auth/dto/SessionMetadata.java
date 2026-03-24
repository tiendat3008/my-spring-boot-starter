package com.spring.starter.auth.dto;

import java.time.Instant;

public record SessionMetadata(String ipAddress, String userAgent, Instant loginTime) {}
