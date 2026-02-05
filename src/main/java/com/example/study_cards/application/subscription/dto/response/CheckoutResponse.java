package com.example.study_cards.application.subscription.dto.response;

public record CheckoutResponse(
        String orderId,
        String customerKey,
        Integer amount,
        String orderName
) {
}
