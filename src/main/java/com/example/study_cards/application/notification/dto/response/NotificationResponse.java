package com.example.study_cards.application.notification.dto.response;

import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Boolean isRead,
        Long referenceId,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getIsRead(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}
