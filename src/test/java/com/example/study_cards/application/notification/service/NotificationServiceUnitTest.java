package com.example.study_cards.application.notification.service;

import com.example.study_cards.application.notification.dto.request.FcmTokenRequest;
import com.example.study_cards.application.notification.dto.request.PushSettingRequest;
import com.example.study_cards.application.notification.dto.response.NotificationResponse;
import com.example.study_cards.application.notification.dto.response.PushSettingResponse;
import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.notification.exception.NotificationErrorCode;
import com.example.study_cards.domain.notification.exception.NotificationException;
import com.example.study_cards.domain.notification.service.NotificationDomainService;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.fcm.service.FcmService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceUnitTest extends BaseUnitTest {

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private StudyRecordDomainService studyRecordDomainService;

    @Mock
    private FcmService fcmService;

    @Mock
    private NotificationDomainService notificationDomainService;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private static final Long USER_ID = 1L;
    private static final String FCM_TOKEN = "test-fcm-token-12345";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(testUser, "id", USER_ID);
    }

    @Nested
    @DisplayName("registerFcmToken")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("FCM 토큰을 등록한다")
        void registerFcmToken_success() {
            // given
            FcmTokenRequest request = new FcmTokenRequest(FCM_TOKEN);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            notificationService.registerFcmToken(USER_ID, request);

            // then
            assertThat(testUser.getFcmToken()).isEqualTo(FCM_TOKEN);
        }
    }

    @Nested
    @DisplayName("removeFcmToken")
    class RemoveFcmTokenTest {

        @Test
        @DisplayName("FCM 토큰을 삭제한다")
        void removeFcmToken_success() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            notificationService.removeFcmToken(USER_ID);

            // then
            assertThat(testUser.getFcmToken()).isNull();
        }
    }

    @Nested
    @DisplayName("getPushSettings")
    class GetPushSettingsTest {

        @Test
        @DisplayName("푸시 설정을 조회한다")
        void getPushSettings_success() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            testUser.updatePushEnabled(true);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            PushSettingResponse result = notificationService.getPushSettings(USER_ID);

            // then
            assertThat(result.pushEnabled()).isTrue();
            assertThat(result.hasFcmToken()).isTrue();
        }

        @Test
        @DisplayName("FCM 토큰이 없는 경우 hasFcmToken은 false를 반환한다")
        void getPushSettings_noToken_returnsFalse() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            PushSettingResponse result = notificationService.getPushSettings(USER_ID);

            // then
            assertThat(result.hasFcmToken()).isFalse();
        }
    }

    @Nested
    @DisplayName("updatePushSettings")
    class UpdatePushSettingsTest {

        @Test
        @DisplayName("푸시 설정을 활성화한다")
        void updatePushSettings_enable() {
            // given
            testUser.updatePushEnabled(false);
            PushSettingRequest request = new PushSettingRequest(true);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            PushSettingResponse result = notificationService.updatePushSettings(USER_ID, request);

            // then
            assertThat(result.pushEnabled()).isTrue();
            assertThat(testUser.getPushEnabled()).isTrue();
        }

        @Test
        @DisplayName("푸시 설정을 비활성화한다")
        void updatePushSettings_disable() {
            // given
            testUser.updatePushEnabled(true);
            PushSettingRequest request = new PushSettingRequest(false);
            given(userDomainService.findById(USER_ID)).willReturn(testUser);

            // when
            PushSettingResponse result = notificationService.updatePushSettings(USER_ID, request);

            // then
            assertThat(result.pushEnabled()).isFalse();
            assertThat(testUser.getPushEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("sendDailyReviewNotifications")
    class SendDailyReviewNotificationsTest {

        @Test
        @DisplayName("복습 대상 카드가 있는 사용자에게 알림을 발송한다")
        void sendDailyReviewNotifications_withDueCards_sendsPush() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            testUser.updatePushEnabled(true);

            User user2 = User.builder()
                    .email("test2@example.com")
                    .password("password123")
                    .nickname("테스터2")
                    .build();
            ReflectionTestUtils.setField(user2, "id", 2L);
            user2.updateFcmToken("token-2");
            user2.updatePushEnabled(true);

            given(userDomainService.findAllPushEnabledUsersWithToken())
                    .willReturn(List.of(testUser, user2));
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(5);
            given(studyRecordDomainService.countDueCards(eq(user2), any(LocalDate.class))).willReturn(0);

            // when
            notificationService.sendDailyReviewNotifications();

            // then
            verify(fcmService, times(1)).sendNotification(eq(FCM_TOKEN), anyString(), anyString());
            verify(fcmService, never()).sendNotification(eq("token-2"), anyString(), anyString());
        }

        @Test
        @DisplayName("복습 대상 카드가 없으면 알림을 발송하지 않는다")
        void sendDailyReviewNotifications_noDueCards_doesNotSendPush() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            testUser.updatePushEnabled(true);

            given(userDomainService.findAllPushEnabledUsersWithToken())
                    .willReturn(List.of(testUser));
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);

            // when
            notificationService.sendDailyReviewNotifications();

            // then
            verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("대상 사용자가 없으면 아무 작업도 하지 않는다")
        void sendDailyReviewNotifications_noTargetUsers_doesNothing() {
            // given
            given(userDomainService.findAllPushEnabledUsersWithToken())
                    .willReturn(List.of());

            // when
            notificationService.sendDailyReviewNotifications();

            // then
            verify(studyRecordDomainService, never()).countDueCards(any(), any());
            verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("sendNotification")
    class SendNotificationTest {

        @Test
        @DisplayName("알림을 DB에 저장하고 FCM을 발송한다")
        void sendNotification_savesAndSendsFcm() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            testUser.updatePushEnabled(true);

            // when
            notificationService.sendNotification(testUser, NotificationType.STREAK_7, "스트릭 달성!", "7일 연속 학습!");

            // then
            verify(notificationDomainService).create(
                    eq(testUser),
                    eq(NotificationType.STREAK_7),
                    eq("스트릭 달성!"),
                    eq("7일 연속 학습!"),
                    isNull()
            );
            verify(fcmService).sendNotification(eq(FCM_TOKEN), eq("스트릭 달성!"), eq("7일 연속 학습!"));
        }

        @Test
        @DisplayName("푸시가 비활성화되면 FCM을 발송하지 않는다")
        void sendNotification_pushDisabled_doesNotSendFcm() {
            // given
            testUser.updateFcmToken(FCM_TOKEN);
            testUser.updatePushEnabled(false);

            // when
            notificationService.sendNotification(testUser, NotificationType.STREAK_7, "스트릭 달성!", "7일 연속 학습!");

            // then
            verify(notificationDomainService).create(
                    eq(testUser),
                    eq(NotificationType.STREAK_7),
                    eq("스트릭 달성!"),
                    eq("7일 연속 학습!"),
                    isNull()
            );
            verify(fcmService, never()).sendNotification(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTest {

        @Test
        @DisplayName("사용자의 알림 목록을 조회한다")
        void getNotifications_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.STREAK_7)
                    .title("스트릭 달성!")
                    .body("7일 연속 학습!")
                    .build();
            ReflectionTestUtils.setField(notification, "id", 1L);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(notificationDomainService.findByUser(testUser, pageable))
                    .willReturn(new PageImpl<>(List.of(notification), pageable, 1));

            // when
            Page<NotificationResponse> result = notificationService.getNotifications(USER_ID, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).type()).isEqualTo(NotificationType.STREAK_7);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCountTest {

        @Test
        @DisplayName("안읽은 알림 수를 조회한다")
        void getUnreadCount_success() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(notificationDomainService.countUnread(testUser)).willReturn(5L);

            // when
            long result = notificationService.getUnreadCount(USER_ID);

            // then
            assertThat(result).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림을 읽음 처리한다")
        void markAsRead_success() {
            // given
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.STREAK_7)
                    .title("스트릭 달성!")
                    .body("7일 연속 학습!")
                    .build();
            ReflectionTestUtils.setField(notification, "id", 1L);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(notificationDomainService.findById(1L)).willReturn(notification);

            // when
            notificationService.markAsRead(USER_ID, 1L);

            // then
            assertThat(notification.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 알림이면 예외를 발생시킨다")
        void markAsRead_notFound_throwsException() {
            // given
            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(notificationDomainService.findById(999L))
                    .willThrow(new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 999L))
                    .isInstanceOf(NotificationException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사용자의 알림이면 예외를 발생시킨다")
        void markAsRead_accessDenied_throwsException() {
            // given
            User otherUser = User.builder()
                    .email("other@example.com")
                    .password("password123")
                    .nickname("다른사용자")
                    .build();
            ReflectionTestUtils.setField(otherUser, "id", 2L);

            Notification notification = Notification.builder()
                    .user(otherUser)
                    .type(NotificationType.STREAK_7)
                    .title("스트릭 달성!")
                    .body("7일 연속 학습!")
                    .build();
            ReflectionTestUtils.setField(notification, "id", 1L);

            given(userDomainService.findById(USER_ID)).willReturn(testUser);
            given(notificationDomainService.findById(1L)).willReturn(notification);

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 1L))
                    .isInstanceOf(NotificationException.class)
                    .extracting("errorCode")
                    .isEqualTo(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("existsNotification")
    class ExistsNotificationTest {

        @Test
        @DisplayName("중복 알림 여부를 확인한다")
        void existsNotification_returnsTrue() {
            // given
            given(notificationDomainService.existsByUserAndTypeAndReferenceId(
                    testUser, NotificationType.CATEGORY_MASTERED, 1L)).willReturn(true);

            // when
            boolean result = notificationService.existsNotification(testUser, NotificationType.CATEGORY_MASTERED, 1L);

            // then
            assertThat(result).isTrue();
        }
    }
}
