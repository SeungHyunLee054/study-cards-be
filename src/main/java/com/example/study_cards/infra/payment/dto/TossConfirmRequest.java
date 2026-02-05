package com.example.study_cards.infra.payment.dto;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        Integer amount
) {
}
