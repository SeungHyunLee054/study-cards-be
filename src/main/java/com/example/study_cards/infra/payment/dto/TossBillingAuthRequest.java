package com.example.study_cards.infra.payment.dto;

public record TossBillingAuthRequest(
        String authKey,
        String customerKey
) {
}
