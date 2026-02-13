package com.example.study_cards.domain.subscription.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlan {

    FREE("무료", 0, 0, true, false, 5),
    PRO("프로", 9900, 99000, true, true, 30);

    private final String displayName;
    private final int monthlyPrice;
    private final int yearlyPrice;
    private final boolean canGenerateAiCards;      // AI 카드 생성 권한
    private final boolean canUseAiRecommendations; // AI 복습 전략 메시지 권한
    private final int aiGenerationDailyLimit;      // AI 생성 제한 (FREE: 평생 5회, PRO: 일일 30회)

    public int getPrice(BillingCycle billingCycle) {
        return billingCycle == BillingCycle.YEARLY ? yearlyPrice : monthlyPrice;
    }
}
