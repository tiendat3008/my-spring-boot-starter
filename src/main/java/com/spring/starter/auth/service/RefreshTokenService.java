package com.spring.starter.auth.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

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
    private static final String USER_SESSIONS_KEY_PREFIX = "refresh:user:";

    public void storeRefreshToken(String userId, String tokenId, Long expiryInSeconds) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, userId);
        redisTemplate.expire(key, Duration.ofSeconds(expiryInSeconds));

        String userSessionsKey = userSessionsKey(userId);
        redisTemplate.opsForSet().add(userSessionsKey, tokenId);
        redisTemplate.expire(userSessionsKey, Duration.ofSeconds(expiryInSeconds));
    }

    public boolean isRefreshTokenValid(String tokenId) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void revokeRefreshToken(String tokenId) {
        String key = REFRESH_KEY_PREFIX + tokenId;

        String userId = redisTemplate.opsForValue().get(key);

        if (userId != null) {
            redisTemplate.opsForSet().remove(userSessionsKey(userId), tokenId);
        }

        redisTemplate.delete(key);
    }

    public List<String> listUserSessionIds(String userId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));

        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
                .filter(tokenId -> Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_KEY_PREFIX + tokenId)))
                .toList();
    }

    public boolean revokeUserSession(String userId, String tokenId) {
        String key = REFRESH_KEY_PREFIX + tokenId;
        String tokenOwnerId = redisTemplate.opsForValue().get(key);

        if (tokenOwnerId == null || !tokenOwnerId.equals(userId)) {
            return false;
        }

        redisTemplate.opsForSet().remove(userSessionsKey(userId), tokenId);
        redisTemplate.delete(key);
        return true;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId + ":sessions";
    }
}
