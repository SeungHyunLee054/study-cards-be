package com.example.study_cards.domain.notification.repository;

import com.example.study_cards.domain.notification.entity.Notification;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    boolean existsByUserAndTypeAndReferenceId(User user, NotificationType type, Long referenceId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.type = :type AND n.referenceId = :referenceId")
    void deleteByTypeAndReferenceId(@Param("type") NotificationType type, @Param("referenceId") Long referenceId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadByUser(@Param("user") User user);
}
