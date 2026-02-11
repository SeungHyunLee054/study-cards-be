package com.example.study_cards.infra.payment.dto.request;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
