package com.example.study_cards.application.payment.dto.response;

import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long id,
        String orderId,
        Integer amount,
        PaymentStatus status,
        PaymentType paymentType,
        SubscriptionPlan planType,
        BillingCycle billingCycle,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getType(),
                payment.getPlan(),
                payment.getBillingCycle(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
