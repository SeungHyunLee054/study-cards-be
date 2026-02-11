package com.example.study_cards.application.notification.scheduler;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.common.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPushScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "${app.notification.daily-push-cron:0 0 8 * * *}")
    @DistributedLock(key = "scheduler:daily-push", ttlMinutes = 30)
    public void sendDailyReviewReminder() {
        log.info("일일 복습 알림 스케줄러 실행");
        try {
            notificationService.sendDailyReviewNotifications();
        } catch (Exception e) {
            log.error("일일 복습 알림 스케줄러 실행 실패", e);
        }
    }
}
