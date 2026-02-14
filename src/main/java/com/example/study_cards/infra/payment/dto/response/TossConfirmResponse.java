package com.example.study_cards.infra.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

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
        JsonNode easyPay
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
}
