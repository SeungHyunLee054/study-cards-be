package com.example.study_cards.application.subscription.dto.response;

import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;

public record PlanResponse(
        SubscriptionPlan plan,
        String displayName,
        int monthlyPrice,
        int yearlyPrice,
        boolean canGenerateAiCards,
        boolean canUseAiRecommendations,
        int aiGenerationDailyLimit,
        boolean isPurchasable
) {
    public static PlanResponse from(SubscriptionPlan plan) {
        return new PlanResponse(
                plan,
                plan.getDisplayName(),
                plan.getMonthlyPrice(),
                plan.getYearlyPrice(),
                plan.isCanGenerateAiCards(),
                plan.isCanUseAiRecommendations(),
                plan.getAiGenerationDailyLimit(),
                plan == SubscriptionPlan.PRO
        );
    }
}
