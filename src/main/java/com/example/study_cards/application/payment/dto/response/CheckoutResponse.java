package com.example.study_cards.application.payment.dto.response;

public record CheckoutResponse(
        String orderId,
        String customerKey,
        Integer amount,
        String orderName
) {
}
