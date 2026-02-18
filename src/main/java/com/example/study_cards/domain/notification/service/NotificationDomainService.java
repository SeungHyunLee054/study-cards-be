package com.example.study_cards.domain.notification.service;

import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.notification.exception.NotificationErrorCode;
import com.example.study_cards.domain.notification.exception.NotificationException;
import com.example.study_cards.domain.notification.repository.NotificationRepository;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationDomainService {

    private final NotificationRepository notificationRepository;

    public Notification create(User user, NotificationType type, String title, String body, Long referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .build();
        return notificationRepository.save(notification);
    }

    public Page<Notification> findByUser(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public Page<Notification> findUnreadByUser(User user, Pageable pageable) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable);
    }

    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    public Notification findById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    public void markAllAsReadByUser(User user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    public boolean existsByUserAndTypeAndReferenceId(User user, NotificationType type, Long referenceId) {
        return notificationRepository.existsByUserAndTypeAndReferenceId(user, type, referenceId);
    }

    public void deleteByTypeAndReferenceId(NotificationType type, Long referenceId) {
        notificationRepository.deleteByTypeAndReferenceId(type, referenceId);
    }
}
