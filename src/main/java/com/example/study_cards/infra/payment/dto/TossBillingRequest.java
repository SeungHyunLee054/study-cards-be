package com.example.study_cards.infra.payment.dto;

public record TossBillingRequest(
        Integer amount,
        String customerKey,
        String orderId,
        String orderName
) {
}
