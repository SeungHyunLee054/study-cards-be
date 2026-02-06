package com.example.study_cards.application.generation.scheduler;

import com.example.study_cards.application.generation.service.GenerationApprovalService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class GenerationApprovalSchedulerTest extends BaseUnitTest {

    @Mock
    private GenerationApprovalService approvalService;

    @InjectMocks
    private GenerationApprovalScheduler scheduler;

    @Nested
    @DisplayName("migrateApprovedCards")
    class MigrateApprovedCardsTest {

        @Test
        @DisplayName("승인된 카드를 이동한다")
        void migrateApprovedCards_success() {
            // given
            given(approvalService.migrateApprovedToCards()).willReturn(5);

            // when
            scheduler.migrateApprovedCards();

            // then
            verify(approvalService).migrateApprovedToCards();
        }

        @Test
        @DisplayName("이동할 카드가 없으면 0을 반환한다")
        void migrateApprovedCards_noCards_returns0() {
            // given
            given(approvalService.migrateApprovedToCards()).willReturn(0);

            // when
            scheduler.migrateApprovedCards();

            // then
            verify(approvalService).migrateApprovedToCards();
        }

        @Test
        @DisplayName("예외가 발생해도 스케줄러는 종료되지 않는다")
        void migrateApprovedCards_exception_continuesExecution() {
            // given
            given(approvalService.migrateApprovedToCards())
                    .willThrow(new RuntimeException("Database error"));

            // when
            scheduler.migrateApprovedCards();

            // then
            verify(approvalService).migrateApprovedToCards();
        }

        @Test
        @DisplayName("여러 번 실행해도 정상 동작한다")
        void migrateApprovedCards_multipleExecutions_success() {
            // given
            given(approvalService.migrateApprovedToCards())
                    .willReturn(3)
                    .willReturn(2)
                    .willReturn(0);

            // when
            scheduler.migrateApprovedCards();
            scheduler.migrateApprovedCards();
            scheduler.migrateApprovedCards();

            // then
            verify(approvalService, times(3)).migrateApprovedToCards();
        }
    }
}
