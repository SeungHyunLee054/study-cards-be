package com.example.study_cards.domain.subscription.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscription_user", columnList = "user_id"),
        @Index(name = "idx_subscription_status", columnList = "status"),
        @Index(name = "idx_subscription_end_date", columnList = "end_date")
})
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(unique = true)
    private String customerKey;

    private String billingKey;

    private String cancelReason;

    @Builder
    public Subscription(User user, SubscriptionPlan plan, SubscriptionStatus status,
                        BillingCycle billingCycle, LocalDateTime startDate, LocalDateTime endDate,
                        String customerKey, String billingKey) {
        this.user = user;
        this.plan = plan;
        this.status = status != null ? status : SubscriptionStatus.PENDING;
        this.billingCycle = billingCycle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.customerKey = customerKey;
        this.billingKey = billingKey;
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE
                && this.endDate.isAfter(LocalDateTime.now());
    }

    public boolean isExpired() {
        return this.endDate.isBefore(LocalDateTime.now());
    }

    public boolean isExpiringSoon(int days) {
        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        return this.endDate.isBefore(threshold) && this.endDate.isAfter(LocalDateTime.now());
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELED;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELED;
        this.cancelReason = reason;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void renew(LocalDateTime newEndDate) {
        this.startDate = LocalDateTime.now();
        this.endDate = newEndDate;
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void updateBillingKey(String billingKey) {
        this.billingKey = billingKey;
    }

    public void changePlan(SubscriptionPlan newPlan, BillingCycle newBillingCycle) {
        this.plan = newPlan;
        this.billingCycle = newBillingCycle;
    }
}
