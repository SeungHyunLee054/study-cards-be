package com.example.study_cards.domain.notification.repository;

import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.example.study_cards.domain.notification.entity.QNotification.notification;

@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteByTypeAndReferenceId(NotificationType type, Long referenceId) {
        queryFactory
                .delete(notification)
                .where(
                        notification.type.eq(type),
                        notification.referenceId.eq(referenceId)
                )
                .execute();
    }

    @Override
    public void markAllAsReadByUser(User user) {
        queryFactory
                .update(notification)
                .set(notification.isRead, true)
                .where(
                        notification.user.eq(user),
                        notification.isRead.eq(false)
                )
                .execute();
    }
}
