package com.example.study_cards.application.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmBillingRequest(
        @NotBlank(message = "authKey는 필수입니다.")
        String authKey,

        @NotBlank(message = "customerKey는 필수입니다.")
        String customerKey,

        @NotBlank(message = "orderId는 필수입니다.")
        String orderId
) {
}
