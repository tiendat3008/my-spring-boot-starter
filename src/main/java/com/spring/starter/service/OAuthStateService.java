package com.spring.starter.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.spring.starter.enums.SocialProvider;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuthStateService {

    StringRedisTemplate redisTemplate;

    private static final String STATE_KEY_PREFIX = "oauth_state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    public String issueState(SocialProvider provider) {
        String state = UUID.randomUUID().toString();
        String key = buildKey(provider, state);

        redisTemplate.opsForValue().set(key, "1", STATE_TTL);

        return state;
    }

    public boolean consumeState(SocialProvider provider, String state) {
        String key = buildKey(provider, state);
        Boolean hasKey = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    private String buildKey(SocialProvider provider, String state) {
        return STATE_KEY_PREFIX + provider.name().toLowerCase() + ":" + state;
    }
}
