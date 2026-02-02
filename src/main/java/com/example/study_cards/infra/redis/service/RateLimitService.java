package com.example.study_cards.infra.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:cards:";
    private static final int MAX_CARDS_PER_DAY = 15;

    private final RedisTemplate<String, Object> redisTemplate;

    public int getRemainingCards(String ipAddress) {
        String key = RATE_LIMIT_PREFIX + ipAddress;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return Math.max(0, MAX_CARDS_PER_DAY - (count != null ? count : 0));
    }

    public void incrementCardCount(String ipAddress, int cardCount) {
        if (cardCount <= 0) {
            return;
        }

        String key = RATE_LIMIT_PREFIX + ipAddress;
        Long currentCount = redisTemplate.opsForValue().increment(key, cardCount);

        if (currentCount != null && currentCount == cardCount) {
            redisTemplate.expire(key, getTtlUntilMidnight());
        }
    }

    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
