package com.spring.starter.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenService {

    StringRedisTemplate redisTemplate;
    private static final String REFRESH_KEY_PREFIX = "refresh:";

    public void storeRefreshToken(String userId, String tokenId, Long expiryInSeconds) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, userId);
        redisTemplate.expire(key, Duration.ofSeconds(expiryInSeconds));
    }

    public boolean isRefreshTokenValid(String tokenId) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void revokeRefreshToken(String tokenId) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        redisTemplate.delete(key);
    }
}
