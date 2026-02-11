package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class DistributedLockService {

    private static final String LOCK_PREFIX = "lock:";

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final RedisTemplate<String, Object> redisTemplate;

    public String tryLock(String lockName, Duration ttl) {
        String key = LOCK_PREFIX + lockName;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, lockValue, ttl);
        return Boolean.TRUE.equals(acquired) ? lockValue : null;
    }

    public void unlock(String lockName, String lockValue) {
        String key = LOCK_PREFIX + lockName;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        redisTemplate.execute(script, List.of(key), lockValue);
    }
}
