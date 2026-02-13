package com.example.study_cards.application.subscription.dto.response;

import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        SubscriptionPlan plan,
        String planDisplayName,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean isActive,
        boolean canGenerateAiCards,
        boolean canUseAiRecommendations,
        int aiGenerationDailyLimit
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlan(),
                subscription.getPlan().getDisplayName(),
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.isActive(),
                subscription.getPlan().isCanGenerateAiCards(),
                subscription.getPlan().isCanUseAiRecommendations(),
                subscription.getPlan().getAiGenerationDailyLimit()
        );
    }
}
