package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "bl:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void blacklistToken(String accessToken, long remainingMs) {
        if (remainingMs > 0) {
            String key = BLACKLIST_PREFIX + accessToken;
            redisTemplate.opsForValue().set(key, "blacklisted", remainingMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
