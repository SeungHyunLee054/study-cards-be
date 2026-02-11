package com.example.study_cards.infra.payment.dto.request;

public record TossBillingRequest(
        Integer amount,
        String customerKey,
        String orderId,
        String orderName
) {
}
