package com.example.study_cards.application.subscription.dto.request;

import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "구독 플랜을 선택해주세요.")
        SubscriptionPlan plan,

        @NotNull(message = "결제 주기를 선택해주세요.")
        BillingCycle billingCycle
) {
}
