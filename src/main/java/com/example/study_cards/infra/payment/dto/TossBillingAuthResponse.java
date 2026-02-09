package com.example.study_cards.infra.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossBillingAuthResponse(
        String billingKey,
        String customerKey,
        String authenticatedAt,
        String method,
        CardInfo card
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardInfo(
            String company,
            String number,
            String cardType,
            String ownerType
    ) {
    }
}
