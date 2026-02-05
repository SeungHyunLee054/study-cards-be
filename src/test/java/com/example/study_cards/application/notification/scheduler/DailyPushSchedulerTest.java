package com.example.study_cards.application.notification.scheduler;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

class DailyPushSchedulerTest extends BaseUnitTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailyPushScheduler dailyPushScheduler;

    @Nested
    @DisplayName("sendDailyReviewReminder")
    class SendDailyReviewReminderTest {

        @Test
        @DisplayName("일일 복습 알림 발송 메서드를 호출한다")
        void sendDailyReviewReminder_callsNotificationService() {
            // when
            dailyPushScheduler.sendDailyReviewReminder();

            // then
            verify(notificationService).sendDailyReviewNotifications();
        }
    }
}
