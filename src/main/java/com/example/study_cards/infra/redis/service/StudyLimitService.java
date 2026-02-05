package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class StudyLimitService {

    private static final String STUDY_COUNT_PREFIX = "study_count:";

    private final RedisTemplate<String, Object> redisTemplate;

    public int getRemainingStudies(Long userId, SubscriptionPlan plan) {
        if (plan.isUnlimited()) {
            return Integer.MAX_VALUE;
        }

        String key = STUDY_COUNT_PREFIX + userId;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        int currentCount = count != null ? count : 0;

        return Math.max(0, plan.getDailyLimit() - currentCount);
    }

    public void incrementStudyCount(Long userId) {
        String key = STUDY_COUNT_PREFIX + userId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, getTtlUntilMidnight());
    }

    public boolean canStudy(Long userId, SubscriptionPlan plan) {
        if (plan.isUnlimited()) {
            return true;
        }

        return getRemainingStudies(userId, plan) > 0;
    }

    public int getTodayStudyCount(Long userId) {
        String key = STUDY_COUNT_PREFIX + userId;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count != null ? count : 0;
    }

    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
