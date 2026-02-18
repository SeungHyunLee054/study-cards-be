package com.example.study_cards.application.ai.scheduler;

import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class AiGenerationLogCleanupSchedulerUnitTest extends BaseUnitTest {

    @Mock
    private AiGenerationLogRepository aiGenerationLogRepository;

    @InjectMocks
    private AiGenerationLogCleanupScheduler scheduler;

    @Nested
    @DisplayName("cleanupOldLogs")
    class CleanupOldLogsTest {

        @Test
        @DisplayName("설정된 보관 기간 기준으로 타입별 로그를 정리한다")
        void cleanupOldLogs_withConfiguredRetention_deletesByType() {
            // given
            ReflectionTestUtils.setField(scheduler, "recommendationRetentionDays", 90L);
            ReflectionTestUtils.setField(scheduler, "userCardRetentionDays", 30L);
            given(aiGenerationLogRepository.deleteByTypeInAndCreatedAtBefore(any(), any())).willReturn(10L);
            given(aiGenerationLogRepository.deleteByTypeAndCreatedAtBefore(any(), any())).willReturn(5L);

            LocalDateTime before = LocalDateTime.now();

            // when
            scheduler.cleanupOldLogs();

            // then
            LocalDateTime after = LocalDateTime.now();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<AiGenerationType>> recommendationTypesCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<LocalDateTime> recommendationCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<AiGenerationType> userCardTypeCaptor = ArgumentCaptor.forClass(AiGenerationType.class);
            ArgumentCaptor<LocalDateTime> userCardCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            verify(aiGenerationLogRepository).deleteByTypeInAndCreatedAtBefore(
                    recommendationTypesCaptor.capture(),
                    recommendationCutoffCaptor.capture()
            );
            verify(aiGenerationLogRepository).deleteByTypeAndCreatedAtBefore(
                    userCardTypeCaptor.capture(),
                    userCardCutoffCaptor.capture()
            );

            assertThat(recommendationTypesCaptor.getValue())
                    .containsExactly(AiGenerationType.RECOMMENDATION, AiGenerationType.WEAKNESS_ANALYSIS);
            assertThat(userCardTypeCaptor.getValue()).isEqualTo(AiGenerationType.USER_CARD);

            assertThat(recommendationCutoffCaptor.getValue())
                    .isBetween(before.minusDays(90).minusSeconds(1), after.minusDays(90).plusSeconds(1));
            assertThat(userCardCutoffCaptor.getValue())
                    .isBetween(before.minusDays(30).minusSeconds(1), after.minusDays(30).plusSeconds(1));
        }

        @Test
        @DisplayName("보관 기간이 1일 미만이면 1일로 보정한다")
        void cleanupOldLogs_withInvalidRetention_clampsToMinimumOneDay() {
            // given
            ReflectionTestUtils.setField(scheduler, "recommendationRetentionDays", 0L);
            ReflectionTestUtils.setField(scheduler, "userCardRetentionDays", -5L);
            given(aiGenerationLogRepository.deleteByTypeInAndCreatedAtBefore(any(), any())).willReturn(1L);
            given(aiGenerationLogRepository.deleteByTypeAndCreatedAtBefore(any(), any())).willReturn(1L);

            LocalDateTime before = LocalDateTime.now();

            // when
            scheduler.cleanupOldLogs();

            // then
            LocalDateTime after = LocalDateTime.now();
            ArgumentCaptor<LocalDateTime> recommendationCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> userCardCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            verify(aiGenerationLogRepository).deleteByTypeInAndCreatedAtBefore(any(), recommendationCutoffCaptor.capture());
            verify(aiGenerationLogRepository).deleteByTypeAndCreatedAtBefore(any(), userCardCutoffCaptor.capture());

            assertThat(recommendationCutoffCaptor.getValue())
                    .isBetween(before.minusDays(1).minusSeconds(1), after.minusDays(1).plusSeconds(1));
            assertThat(userCardCutoffCaptor.getValue())
                    .isBetween(before.minusDays(1).minusSeconds(1), after.minusDays(1).plusSeconds(1));
        }
    }
}
