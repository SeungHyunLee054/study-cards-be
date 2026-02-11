package com.example.study_cards.application.notification.service;

import com.example.study_cards.application.notification.dto.request.FcmTokenRequest;
import com.example.study_cards.application.notification.dto.request.PushSettingRequest;
import com.example.study_cards.application.notification.dto.response.NotificationResponse;
import com.example.study_cards.application.notification.dto.response.PushSettingResponse;
import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.notification.exception.NotificationErrorCode;
import com.example.study_cards.domain.notification.exception.NotificationException;
import com.example.study_cards.domain.notification.repository.NotificationRepository;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final UserDomainService userDomainService;
    private final StudyDomainService studyDomainService;
    private final FcmService fcmService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void registerFcmToken(Long userId, FcmTokenRequest request) {
        User user = userDomainService.findById(userId);
        user.updateFcmToken(request.fcmToken());
        log.info("FCM 토큰 등록 - userId: {}", userId);
    }

    @Transactional
    public void removeFcmToken(Long userId) {
        User user = userDomainService.findById(userId);
        user.removeFcmToken();
        log.info("FCM 토큰 삭제 - userId: {}", userId);
    }

    public PushSettingResponse getPushSettings(Long userId) {
        User user = userDomainService.findById(userId);
        return PushSettingResponse.from(user);
    }

    @Transactional
    public PushSettingResponse updatePushSettings(Long userId, PushSettingRequest request) {
        User user = userDomainService.findById(userId);
        user.updatePushEnabled(request.pushEnabled());
        log.info("푸시 설정 변경 - userId: {}, pushEnabled: {}", userId, request.pushEnabled());
        return PushSettingResponse.from(user);
    }

    public void sendDailyReviewNotifications() {
        List<User> targetUsers = userDomainService.findAllPushEnabledUsersWithToken();
        LocalDate today = LocalDate.now();

        log.info("일일 복습 알림 발송 시작 - 대상 사용자 수: {}", targetUsers.size());

        int sentCount = 0;
        for (User user : targetUsers) {
            int dueCount = studyDomainService.countDueCards(user, today);
            if (dueCount > 0) {
                String title = "오늘의 복습 알림";
                String body = String.format("복습할 카드가 %d개 있습니다. 지금 학습을 시작해보세요!", dueCount);
                sendNotification(user, NotificationType.DAILY_REVIEW, title, body);
                sentCount++;
            }
        }

        log.info("일일 복습 알림 발송 완료 - 발송 수: {}", sentCount);
    }

    @Transactional
    public void sendNotification(User user, NotificationType type, String title, String body) {
        sendNotification(user, type, title, body, null);
    }

    @Transactional
    public void sendNotification(User user, NotificationType type, String title, String body, Long referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);
        log.info("알림 저장 - userId: {}, type: {}", user.getId(), type);

        if (user.getPushEnabled() && user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            try {
                fcmService.sendNotification(user.getFcmToken(), title, body);
            } catch (Exception e) {
                log.warn("FCM 발송 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
            }
        }
    }

    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        User user = userDomainService.findById(userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::from);
    }

    public Page<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable) {
        User user = userDomainService.findById(userId);
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(Long userId) {
        User user = userDomainService.findById(userId);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        User user = userDomainService.findById(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
        log.info("알림 읽음 처리 - userId: {}, notificationId: {}", userId, notificationId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userDomainService.findById(userId);
        notificationRepository.markAllAsReadByUser(user);
        log.info("전체 알림 읽음 처리 - userId: {}", userId);
    }

    public boolean existsNotification(User user, NotificationType type, Long referenceId) {
        return notificationRepository.existsByUserAndTypeAndReferenceId(user, type, referenceId);
    }

    @Transactional
    public void deleteNotificationsByTypeAndReference(NotificationType type, Long referenceId) {
        notificationRepository.deleteByTypeAndReferenceId(type, referenceId);
        log.info("알림 삭제 - type: {}, referenceId: {}", type, referenceId);
    }

}
