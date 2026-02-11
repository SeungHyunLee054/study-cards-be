package com.example.study_cards.infra.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookPayload(
        String eventType,
        String createdAt,
        DataPayload data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataPayload(
            String paymentKey,
            String orderId,
            String status,
            Integer totalAmount,
            String method,
            String approvedAt,
            String canceledAt,
            String cancelReason,
            String billingKey
    ) {
    }
}
