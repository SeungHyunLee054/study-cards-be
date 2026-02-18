package com.example.study_cards.infra.redis.service;

import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.service.AiGenerationLogDomainService;
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
public class AiLimitService {

    private static final String AI_DAILY_PREFIX = "ai_generation:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final AiGenerationLogDomainService aiGenerationLogDomainService;

    public boolean canGenerate(Long userId, SubscriptionPlan plan) {
        int limit = plan.getAiGenerationDailyLimit();
        int used = getUsedCount(userId, plan);
        return used < limit;
    }

    public int getUsedCount(Long userId, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            // FREE: DB에서 평생 사용량 조회 (유실 방지)
            return (int) aiGenerationLogDomainService.countByUserIdAndTypeAndSuccessTrue(
                    userId, AiGenerationType.USER_CARD);
        }

        // PRO: Redis에서 일일 사용량 조회
        try {
            String key = AI_DAILY_PREFIX + userId;
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Redis AI 카운트 조회 실패: userId={}", userId);
            return 0;
        }
    }

    public int getRemainingCount(Long userId, SubscriptionPlan plan) {
        int limit = plan.getAiGenerationDailyLimit();
        int used = getUsedCount(userId, plan);
        return Math.max(0, limit - used);
    }

    /**
     * PRO 유저: Redis INCR로 슬롯 선점 후 한도 초과 시 DECR 롤백.
     * FREE 유저: DB 기반 canGenerate()로 확인.
     */
    public boolean tryAcquireSlot(Long userId, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            return canGenerate(userId, plan);
        }

        try {
            String key = AI_DAILY_PREFIX + userId;
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, getTtlUntilMidnight());

            if (count != null && count > plan.getAiGenerationDailyLimit()) {
                redisTemplate.opsForValue().decrement(key);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis AI 슬롯 선점 실패: userId={}", userId);
            return false;
        }
    }

    public void releaseSlot(Long userId, SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            return;
        }

        try {
            String key = AI_DAILY_PREFIX + userId;
            redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("Redis AI 슬롯 해제 실패: userId={}", userId);
        }
    }

    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}
