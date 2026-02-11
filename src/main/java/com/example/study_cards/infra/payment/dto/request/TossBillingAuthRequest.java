package com.example.study_cards.infra.payment.dto.request;

public record TossBillingAuthRequest(
        String authKey,
        String customerKey
) {
}
