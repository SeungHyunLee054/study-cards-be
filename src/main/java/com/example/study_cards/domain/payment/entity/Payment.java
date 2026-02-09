package com.example.study_cards.domain.payment.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user", columnList = "user_id"),
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_payment_key", columnList = "payment_key")
})
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(unique = true)
    private String paymentKey;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    private String method;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    private String cancelReason;

    private String failReason;

    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    private String customerKey;

    @Builder
    public Payment(User user, Subscription subscription, String orderId, String paymentKey,
                   Integer amount, PaymentStatus status, PaymentType type, String method,
                   LocalDateTime paidAt, SubscriptionPlan plan, BillingCycle billingCycle,
                   String customerKey) {
        this.user = user;
        this.subscription = subscription;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.type = type;
        this.method = method;
        this.paidAt = paidAt;
        this.plan = plan;
        this.billingCycle = billingCycle;
        this.customerKey = customerKey;
    }

    public void complete(String paymentKey, String method, LocalDateTime paidAt) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.paidAt = paidAt;
        this.status = PaymentStatus.COMPLETED;
    }

    public void cancel(String reason) {
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();
        this.status = PaymentStatus.CANCELED;
    }

    public void fail(String reason) {
        this.failReason = reason;
        this.status = PaymentStatus.FAILED;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public void linkSubscription(Subscription subscription) {
        this.subscription = subscription;
    }
}
