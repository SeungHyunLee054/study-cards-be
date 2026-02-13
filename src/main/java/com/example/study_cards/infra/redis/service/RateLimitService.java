package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:cards:";
    private static final int MAX_CARDS_PER_DAY = 15;

    private final RedisTemplate<String, Object> redisTemplate;

    public int getRemainingCards(String ipAddress) {
        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            return Math.max(0, MAX_CARDS_PER_DAY - (count != null ? count : 0));
        } catch (Exception e) {
            log.warn("Redis 장애로 rate limit 조회 실패 - ip: {}, 허용 처리", ipAddress, e);
            return MAX_CARDS_PER_DAY;
        }
    }

    public void incrementCardCount(String ipAddress, int cardCount) {
        if (cardCount <= 0) {
            return;
        }

        try {
            String key = RATE_LIMIT_PREFIX + ipAddress;
            redisTemplate.opsForValue().increment(key, cardCount);
            redisTemplate.expire(key, getTtlUntilMidnight());
        } catch (Exception e) {
            log.warn("Redis 장애로 rate limit 증가 실패 - ip: {}", ipAddress, e);
        }
    }

    public boolean isRateLimited(String action, String identifier, int maxAttempts, Duration window) {
        try {
            String key = "rate_limit:" + action + ":" + identifier;
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, window);
            return count != null && count > maxAttempts;
        } catch (Exception e) {
            log.warn("Redis 장애로 rate limit 확인 실패 - action: {}, identifier: {}, 허용 처리", action, identifier, e);
            return false;
        }
    }

    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
