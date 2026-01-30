package com.example.study_cards.infra.redis.service;

import com.example.study_cards.infra.redis.vo.UserVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class UserCacheService {

    private static final String USER_CACHE_PREFIX = "user:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheUser(UserVo userVo, long ttlMs) {
        String key = USER_CACHE_PREFIX + userVo.id();
        redisTemplate.opsForValue().set(key, userVo, ttlMs, TimeUnit.MILLISECONDS);
    }

    public Optional<UserVo> getCachedUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof UserVo userVo) {
            return Optional.of(userVo);
        }
        return Optional.empty();
    }

    public void evictUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
