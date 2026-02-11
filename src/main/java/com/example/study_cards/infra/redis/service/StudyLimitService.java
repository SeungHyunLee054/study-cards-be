package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
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
public class StudyLimitService {

    private static final String STUDY_COUNT_PREFIX = "study_count:";

    private final RedisTemplate<String, Object> redisTemplate;

    public int getRemainingStudies(Long userId, SubscriptionPlan plan) {
        if (plan.isUnlimited()) {
            return Integer.MAX_VALUE;
        }

        try {
            String key = STUDY_COUNT_PREFIX + userId;
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            int currentCount = count != null ? count : 0;
            return Math.max(0, plan.getDailyLimit() - currentCount);
        } catch (Exception e) {
            log.warn("Redis 조회 실패, 학습 허용: userId={}", userId);
            return plan.getDailyLimit();
        }
    }

    public void incrementStudyCount(Long userId) {
        try {
            String key = STUDY_COUNT_PREFIX + userId;
            redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, getTtlUntilMidnight());
        } catch (Exception e) {
            log.warn("Redis 학습 카운트 증가 실패: userId={}", userId);
        }
    }

    public boolean canStudy(Long userId, SubscriptionPlan plan) {
        if (plan.isUnlimited()) {
            return true;
        }

        return getRemainingStudies(userId, plan) > 0;
    }

    public int getTodayStudyCount(Long userId) {
        try {
            String key = STUDY_COUNT_PREFIX + userId;
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Redis 학습 카운트 조회 실패: userId={}", userId);
            return 0;
        }
    }

    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
