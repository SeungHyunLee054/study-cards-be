package com.example.study_cards.domain.subscription.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingCycle {

    MONTHLY("월간", 1),
    YEARLY("연간", 12);

    private final String displayName;
    private final int months;
}
