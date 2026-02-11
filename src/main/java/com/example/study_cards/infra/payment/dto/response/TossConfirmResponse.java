package com.example.study_cards.infra.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status,
        Integer totalAmount,
        String method,
        String requestedAt,
        String approvedAt,
        CardInfo card,
        EasyPayInfo easyPay
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardInfo(
            String company,
            String number,
            String cardType,
            String ownerType,
            String billingKey
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EasyPayInfo(
            String provider,
            Integer amount,
            Integer discountAmount
    ) {
    }
}
