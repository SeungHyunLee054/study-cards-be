package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long expirationMs) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expirationMs, TimeUnit.MILLISECONDS);
    }

    public Optional<String> getRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value).map(Object::toString);
    }

    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public boolean validateRefreshToken(Long userId, String refreshToken) {
        return getRefreshToken(userId)
                .map(storedToken -> storedToken.equals(refreshToken))
                .orElse(false);
    }
}
