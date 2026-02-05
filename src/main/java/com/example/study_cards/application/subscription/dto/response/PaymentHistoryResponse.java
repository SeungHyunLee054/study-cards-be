package com.example.study_cards.application.subscription.dto.response;

import com.example.study_cards.domain.subscription.entity.Payment;
import com.example.study_cards.domain.subscription.entity.PaymentStatus;
import com.example.study_cards.domain.subscription.entity.PaymentType;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        Long id,
        String orderId,
        Integer amount,
        PaymentStatus status,
        PaymentType type,
        String method,
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
                payment.getMethod(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
