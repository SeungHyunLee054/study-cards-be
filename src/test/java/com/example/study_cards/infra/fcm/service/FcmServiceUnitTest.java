package com.example.study_cards.infra.fcm.service;

import com.example.study_cards.domain.notification.exception.NotificationException;
import com.example.study_cards.support.BaseUnitTest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class FcmServiceUnitTest extends BaseUnitTest {

    private final FcmService fcmService = new FcmService();

    @Nested
    @DisplayName("sendNotification")
    class SendNotificationTest {

        @Test
        @DisplayName("FCM 푸시 알림 발송 성공")
        void sendNotification_success() throws Exception {
            // given
            String fcmToken = "test_fcm_token_12345";
            String title = "Test Title";
            String body = "Test Body";

            try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
                FirebaseMessaging mockInstance = mock(FirebaseMessaging.class);
                firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
                given(mockInstance.send(any(Message.class))).willReturn("message_id_123");

                // when & then (no exception)
                fcmService.sendNotification(fcmToken, title, body);

                verify(mockInstance).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("FCM 발송 실패 시 NotificationException을 발생시킨다")
        void sendNotification_failure_throwsException() throws Exception {
            // given
            String fcmToken = "test_fcm_token_12345";
            String title = "Test Title";
            String body = "Test Body";

            try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
                FirebaseMessaging mockInstance = mock(FirebaseMessaging.class);
                firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);

                FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
                given(mockException.getMessage()).willReturn("Invalid token");
                given(mockInstance.send(any(Message.class))).willThrow(mockException);

                // when & then
                assertThatThrownBy(() -> fcmService.sendNotification(fcmToken, title, body))
                        .isInstanceOf(NotificationException.class);
            }
        }

        @Test
        @DisplayName("짧은 토큰도 정상 처리된다")
        void sendNotification_shortToken_success() throws Exception {
            // given
            String fcmToken = "short";
            String title = "Test Title";
            String body = "Test Body";

            try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {
                FirebaseMessaging mockInstance = mock(FirebaseMessaging.class);
                firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
                given(mockInstance.send(any(Message.class))).willReturn("message_id_123");

                // when & then (no exception)
                fcmService.sendNotification(fcmToken, title, body);

                verify(mockInstance).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("null 토큰은 예외를 발생시킨다")
        void sendNotification_nullToken_throwsException() throws Exception {
            // given
            String title = "Test Title";
            String body = "Test Body";

            // when & then - null 토큰으로 Message 빌드 시 예외 발생
            assertThatThrownBy(() -> fcmService.sendNotification(null, title, body))
                    .isInstanceOf(Exception.class);
        }
    }
}
