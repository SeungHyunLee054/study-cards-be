package com.example.study_cards.application.subscription.service;

import com.example.study_cards.application.subscription.dto.request.CancelSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.request.ResumeSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.ResumeSubscriptionPrepareResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.service.TossPaymentService;
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
    private final TossPaymentService tossPaymentService;

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

    public ResumeSubscriptionPrepareResponse prepareResumeSubscription(User user) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());

        if (!subscription.isActive()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        if (subscription.getBillingCycle() != BillingCycle.MONTHLY) {
            throw new SubscriptionException(SubscriptionErrorCode.YEARLY_SUBSCRIPTION_CANNOT_BE_RESUMED);
        }

        if (!subscription.isAutoRenewalDisabled()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_ALREADY_ENABLED);
        }

        if (subscription.getCustomerKey() == null || subscription.getCustomerKey().isBlank()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_CANNOT_BE_RESUMED);
        }

        return new ResumeSubscriptionPrepareResponse(subscription.getCustomerKey());
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(User user, CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());

        if (!subscription.isActive()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        if (subscription.getBillingCycle() != BillingCycle.MONTHLY) {
            throw new SubscriptionException(SubscriptionErrorCode.YEARLY_SUBSCRIPTION_CANNOT_BE_CANCELED);
        }

        if (subscription.isAutoRenewalDisabled() || subscription.getBillingKey() == null) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_ALREADY_DISABLED);
        }

        subscriptionDomainService.disableAutoRenewal(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse resumeSubscription(User user) {
        return resumeSubscription(user, new ResumeSubscriptionRequest(null));
    }

    @Transactional
    public SubscriptionResponse resumeSubscription(User user, ResumeSubscriptionRequest request) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());

        if (!subscription.isActive()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        if (subscription.getBillingCycle() != BillingCycle.MONTHLY) {
            throw new SubscriptionException(SubscriptionErrorCode.YEARLY_SUBSCRIPTION_CANNOT_BE_RESUMED);
        }

        if (!subscription.isAutoRenewalDisabled()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_ALREADY_ENABLED);
        }

        if (subscription.getBillingKey() == null) {
            issueAndUpdateBillingKey(subscription, request);
        }

        subscriptionDomainService.enableAutoRenewal(subscription);
        return SubscriptionResponse.from(subscription);
    }

    private void issueAndUpdateBillingKey(Subscription subscription, ResumeSubscriptionRequest request) {
        if (request == null || request.authKey() == null || request.authKey().isBlank()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_CANNOT_BE_RESUMED);
        }

        if (subscription.getCustomerKey() == null || subscription.getCustomerKey().isBlank()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_CANNOT_BE_RESUMED);
        }

        String issuedBillingKey;
        try {
            issuedBillingKey = tossPaymentService.issueBillingKey(
                    request.authKey(),
                    subscription.getCustomerKey()
            ).billingKey();
        } catch (SubscriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_CANNOT_BE_RESUMED);
        }

        if (issuedBillingKey == null || issuedBillingKey.isBlank()) {
            throw new SubscriptionException(SubscriptionErrorCode.AUTO_RENEWAL_CANNOT_BE_RESUMED);
        }

        subscriptionDomainService.updateBillingKey(subscription, issuedBillingKey);
    }
}
