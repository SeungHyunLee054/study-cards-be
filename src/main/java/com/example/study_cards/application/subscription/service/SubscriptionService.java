package com.example.study_cards.application.subscription.service;

import com.example.study_cards.application.subscription.dto.request.CancelSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final SubscriptionRepository subscriptionRepository;

    public List<PlanResponse> getPlans() {
        return Arrays.stream(SubscriptionPlan.values())
                .map(PlanResponse::from)
                .toList();
    }

    public SubscriptionResponse getMySubscription(User user) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());
        return SubscriptionResponse.from(subscription);
    }

    public SubscriptionResponse getMySubscriptionOrNull(User user) {
        return subscriptionRepository.findActiveByUserId(user.getId())
                .map(SubscriptionResponse::from)
                .orElse(null);
    }

    @Transactional
    public void cancelSubscription(User user, CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());

        if (!subscription.isActive()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        subscriptionDomainService.cancelSubscription(subscription, request.reason());
    }
}
