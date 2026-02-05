package com.example.study_cards.domain.notification.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_user_type_ref", columnList = "user_id, type, reference_id")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false)
    private Boolean isRead = false;

    private Long referenceId;

    @Builder
    public Notification(User user, NotificationType type, String title, String body, Long referenceId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceId = referenceId;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
