package com.example.study_cards.common.aop;

import com.example.study_cards.infra.redis.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final DistributedLockService distributedLockService;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = distributedLock.key();
        Duration ttl = Duration.ofMinutes(distributedLock.ttlMinutes());

        String lockValue = distributedLockService.tryLock(key, ttl);
        if (lockValue == null) {
            log.info("Failed to acquire lock '{}', skipping execution of {}", key, joinPoint.getSignature().getName());
            return null;
        }

        try {
            return joinPoint.proceed();
        } finally {
            distributedLockService.unlock(key, lockValue);
        }
    }
}
