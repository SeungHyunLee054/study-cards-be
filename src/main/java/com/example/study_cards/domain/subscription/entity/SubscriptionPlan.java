package com.example.study_cards.domain.subscription.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlan {

    FREE("무료", 15, 0, 0, false),
    BASIC("기본", 100, 0, 0, false),
    PREMIUM("프리미엄", Integer.MAX_VALUE, 3900, 39000, true);

    private final String displayName;
    private final int dailyLimit;
    private final int monthlyPrice;
    private final int yearlyPrice;
    private final boolean canAccessAiCards;

    public boolean isUnlimited() {
        return this.dailyLimit == Integer.MAX_VALUE;
    }

    public int getPrice(BillingCycle billingCycle) {
        return billingCycle == BillingCycle.YEARLY ? yearlyPrice : monthlyPrice;
    }
}
