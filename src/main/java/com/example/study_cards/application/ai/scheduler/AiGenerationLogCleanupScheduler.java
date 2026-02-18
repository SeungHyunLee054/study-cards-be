package com.example.study_cards.application.ai.scheduler;

import com.example.study_cards.common.aop.DistributedLock;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGenerationLogCleanupScheduler {

    private static final List<AiGenerationType> RECOMMENDATION_LOG_TYPES =
            List.of(AiGenerationType.RECOMMENDATION, AiGenerationType.WEAKNESS_ANALYSIS);
    private static final long MIN_RETENTION_DAYS = 1L;

    private final AiGenerationLogRepository aiGenerationLogRepository;

    @Value("${app.ai.log-retention.recommendation-days:90}")
    private long recommendationRetentionDays;

    @Value("${app.ai.log-retention.user-card-days:30}")
    private long userCardRetentionDays;

    @Transactional
    @Scheduled(cron = "${app.ai.log-retention.cleanup-cron:0 30 4 * * *}")
    @DistributedLock(key = "scheduler:ai-log-cleanup", ttlMinutes = 30)
    public void cleanupOldLogs() {
        try {
            long recommendationDays = normalizeRetentionDays("recommendation-days", recommendationRetentionDays);
            long userCardDays = normalizeRetentionDays("user-card-days", userCardRetentionDays);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime recommendationCutoff = now.minusDays(recommendationDays);
            LocalDateTime userCardCutoff = now.minusDays(userCardDays);

            long recommendationDeleted = aiGenerationLogRepository.deleteByTypeInAndCreatedAtBefore(
                    RECOMMENDATION_LOG_TYPES,
                    recommendationCutoff
            );
            long userCardDeleted = aiGenerationLogRepository.deleteByTypeAndCreatedAtBefore(
                    AiGenerationType.USER_CARD,
                    userCardCutoff
            );

            log.info("AI 로그 정리 완료 - recommendationDeleted: {}, userCardDeleted: {}", recommendationDeleted, userCardDeleted);
        } catch (Exception e) {
            log.error("AI 로그 정리 스케줄러 실패", e);
        }
    }

    private long normalizeRetentionDays(String propertyName, long configuredDays) {
        if (configuredDays < MIN_RETENTION_DAYS) {
            log.warn("잘못된 AI 로그 보관 기간 설정({}={}), {}일로 보정합니다.",
                    propertyName, configuredDays, MIN_RETENTION_DAYS);
            return MIN_RETENTION_DAYS;
        }
        return configuredDays;
    }
}
