package com.example.study_cards.domain.notification.repository;

import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.user.entity.User;

public interface NotificationRepositoryCustom {

    void deleteByTypeAndReferenceId(NotificationType type, Long referenceId);

    void markAllAsReadByUser(User user);
}
