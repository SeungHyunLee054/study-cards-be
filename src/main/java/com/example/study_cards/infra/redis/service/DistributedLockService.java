package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class DistributedLockService {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;

    public boolean tryLock(String lockName, Duration ttl) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockName);

        // 기존 SETNX 기반 동작과 동일하게 같은 스레드의 재진입도 실패로 처리한다.
        if (lock.isHeldByCurrentThread()) {
            return false;
        }

        try {
            return lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while acquiring lock: {}", lockName, e);
            return false;
        }
    }

    public void unlock(String lockName) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
