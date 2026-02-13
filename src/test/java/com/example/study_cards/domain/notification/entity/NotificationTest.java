package com.example.study_cards.domain.notification.entity;

import com.example.study_cards.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("isRead 기본값은 false이다")
        void builder_isRead_기본값false() {
            // when
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.DAILY_REVIEW)
                    .title("학습 알림")
                    .body("오늘의 학습을 시작하세요!")
                    .build();

            // then
            assertThat(notification.getIsRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTest {

        @Test
        @DisplayName("읽음 처리하면 isRead가 true가 된다")
        void markAsRead_isRead_true() {
            // given
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.DAILY_REVIEW)
                    .title("학습 알림")
                    .body("오늘의 학습을 시작하세요!")
                    .build();

            // when
            notification.markAsRead();

            // then
            assertThat(notification.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("이미 읽은 알림을 다시 읽음 처리해도 true를 유지한다")
        void markAsRead_이미읽음_true유지() {
            // given
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.DAILY_REVIEW)
                    .title("학습 알림")
                    .body("오늘의 학습을 시작하세요!")
                    .build();
            notification.markAsRead();

            // when
            notification.markAsRead();

            // then
            assertThat(notification.getIsRead()).isTrue();
        }
    }
}
