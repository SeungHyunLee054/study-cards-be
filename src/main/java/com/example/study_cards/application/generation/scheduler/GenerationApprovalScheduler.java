package com.example.study_cards.application.generation.scheduler;

import com.example.study_cards.application.generation.service.GenerationApprovalService;
import com.example.study_cards.common.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationApprovalScheduler {

    private final GenerationApprovalService approvalService;

    @Scheduled(cron = "${app.generation.migrate-cron:0 0 3 * * *}")
    @DistributedLock(key = "scheduler:migrate-approved", ttlMinutes = 30)
    public void migrateApprovedCards() {
        try {
            log.info("승인된 카드 이동 스케줄러 시작");
            int count = approvalService.migrateApprovedToCards();
            log.info("승인된 카드 이동 스케줄러 완료 - 이동된 카드 수: {}", count);
        } catch (Exception e) {
            log.error("승인된 카드 이동 스케줄러 실패", e);
        }
    }
}
