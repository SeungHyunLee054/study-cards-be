package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.subscription.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewQuotaService {

    private static final String AI_REVIEW_MONTHLY_PREFIX = "ai_review_monthly:";
    private static final int MONTHLY_LIMIT = 100;

    private final RedisTemplate<String, Object> redisTemplate;

    public ReviewQuota getQuota(Long userId, Subscription subscription) {
        Window window = calculateWindow(subscription);
        int used = getUsedCount(userId, window.keySuffix());
        int remaining = Math.max(0, MONTHLY_LIMIT - used);
        return new ReviewQuota(MONTHLY_LIMIT, used, remaining, window.resetAt());
    }

    public boolean tryAcquireSlot(Long userId, Subscription subscription) {
        Window window = calculateWindow(subscription);
        String key = buildKey(userId, window.keySuffix());

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, ttlUntil(window.resetAt()));

            if (count != null && count > MONTHLY_LIMIT) {
                redisTemplate.opsForValue().decrement(key);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("AI 복습 슬롯 선점 실패: userId={}", userId);
            return false;
        }
    }

    public void releaseSlot(Long userId, Subscription subscription) {
        Window window = calculateWindow(subscription);
        String key = buildKey(userId, window.keySuffix());

        try {
            redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("AI 복습 슬롯 해제 실패: userId={}", userId);
        }
    }

    private int getUsedCount(Long userId, String keySuffix) {
        String key = buildKey(userId, keySuffix);
        try {
            Object count = redisTemplate.opsForValue().get(key);
            if (count instanceof Integer integerCount) {
                return integerCount;
            }
            if (count instanceof Long longCount) {
                return longCount.intValue();
            }
            return 0;
        } catch (Exception e) {
            log.warn("AI 복습 카운트 조회 실패: userId={}", userId);
            return 0;
        }
    }

    private String buildKey(Long userId, String keySuffix) {
        return AI_REVIEW_MONTHLY_PREFIX + userId + ":" + keySuffix;
    }

    private Duration ttlUntil(LocalDateTime resetAt) {
        Duration duration = Duration.between(LocalDateTime.now(), resetAt);
        return duration.isNegative() || duration.isZero() ? Duration.ofHours(1) : duration;
    }

    private Window calculateWindow(Subscription subscription) {
        int billingDay = subscription.getStartDate().getDayOfMonth();
        LocalDate today = LocalDate.now();

        LocalDate thisMonthAnchor = anchorDate(YearMonth.from(today), billingDay);
        LocalDate windowStart = today.isBefore(thisMonthAnchor)
                ? anchorDate(YearMonth.from(today).minusMonths(1), billingDay)
                : thisMonthAnchor;
        LocalDateTime resetAt = anchorDate(YearMonth.from(windowStart).plusMonths(1), billingDay)
                .atStartOfDay();

        return new Window(windowStart.toString(), resetAt);
    }

    private LocalDate anchorDate(YearMonth ym, int billingDay) {
        int safeDay = Math.min(Math.max(1, billingDay), ym.lengthOfMonth());
        return ym.atDay(safeDay);
    }

    private record Window(String keySuffix, LocalDateTime resetAt) {}

    public record ReviewQuota(
            int limit,
            int used,
            int remaining,
            LocalDateTime resetAt
    ) {}
}
