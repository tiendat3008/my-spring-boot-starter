package com.spring.starter.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.spring.starter.auth.dto.SessionMetadata;

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
    private static final String SESSION_META_KEY_PREFIX = "refresh:meta:";

    public void storeRefreshToken(String userId, String tokenId, Long expiryInSeconds, SessionMetadata metadata) {
        Duration ttl = Duration.ofSeconds(expiryInSeconds);
        String key = REFRESH_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, userId);
        redisTemplate.expire(key, ttl);

        storeSessionMetadata(tokenId, metadata, ttl);

        String userSessionsKey = userSessionsKey(userId);
        redisTemplate.opsForSet().add(userSessionsKey, tokenId);
        redisTemplate.expire(userSessionsKey, ttl);
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
        redisTemplate.delete(sessionMetadataKey(tokenId));
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
        redisTemplate.delete(sessionMetadataKey(tokenId));
        return true;
    }

    public SessionMetadata getSessionMetadata(String tokenId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(sessionMetadataKey(tokenId));

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        String loginTimeValue = asString(entries.get("loginTime"));
        Instant loginTime = null;

        if (loginTimeValue != null) {
            try {
                loginTime = Instant.parse(loginTimeValue);
            } catch (DateTimeParseException ignored) {
                loginTime = null;
            }
        }

        return new SessionMetadata(asString(entries.get("ipAddress")), asString(entries.get("userAgent")), loginTime);
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId + ":sessions";
    }

    private void storeSessionMetadata(String tokenId, SessionMetadata metadata, Duration ttl) {
        if (metadata == null) {
            return;
        }

        Map<String, String> metadataMap = new HashMap<>();

        if (metadata.ipAddress() != null && !metadata.ipAddress().isBlank()) {
            metadataMap.put("ipAddress", metadata.ipAddress());
        }

        if (metadata.userAgent() != null && !metadata.userAgent().isBlank()) {
            metadataMap.put("userAgent", metadata.userAgent());
        }

        if (metadata.loginTime() != null) {
            metadataMap.put("loginTime", metadata.loginTime().toString());
        }

        if (metadataMap.isEmpty()) {
            return;
        }

        String key = sessionMetadataKey(tokenId);
        redisTemplate.opsForHash().putAll(key, metadataMap);
        redisTemplate.expire(key, ttl);
    }

    private String sessionMetadataKey(String tokenId) {
        return SESSION_META_KEY_PREFIX + tokenId;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
