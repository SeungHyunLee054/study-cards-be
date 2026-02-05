package com.example.study_cards.application.subscription.dto.response;

import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;

public record PlanResponse(
        SubscriptionPlan plan,
        String displayName,
        int dailyLimit,
        int monthlyPrice,
        int yearlyPrice,
        boolean canAccessAiCards,
        boolean isPurchasable
) {
    public static PlanResponse from(SubscriptionPlan plan) {
        return new PlanResponse(
                plan,
                plan.getDisplayName(),
                plan.getDailyLimit(),
                plan.getMonthlyPrice(),
                plan.getYearlyPrice(),
                plan.isCanAccessAiCards(),
                plan == SubscriptionPlan.PREMIUM
        );
    }
}
