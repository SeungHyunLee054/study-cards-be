package com.example.study_cards.domain.subscription.service;

import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SubscriptionDomainService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionPlan getEffectivePlan(User user) {
        if (user.hasRole(Role.ROLE_ADMIN)) {
            return SubscriptionPlan.PRO;
        }
        return subscriptionRepository.findActiveByUserId(user.getId())
                .map(Subscription::getPlan)
                .orElse(SubscriptionPlan.FREE);
    }

    public Subscription getSubscription(Long userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    public boolean hasActiveSubscription(Long userId) {
        return subscriptionRepository.existsActiveByUserId(userId);
    }

    public void cancelSubscription(Subscription subscription, String reason) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED);
        }
        subscription.cancel(reason);
        subscriptionRepository.save(subscription);
    }

    public void renewSubscription(Subscription subscription) {
        LocalDateTime newEndDate = calculateEndDate(LocalDateTime.now(), subscription.getBillingCycle());
        subscription.renew(newEndDate);
        subscriptionRepository.save(subscription);
    }

    public void expireSubscription(Subscription subscription) {
        subscription.expire();
        subscriptionRepository.save(subscription);
    }

    public List<Subscription> findRenewableSubscriptions(int daysBeforeExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(daysBeforeExpiry);
        return subscriptionRepository.findRenewableSubscriptions(now, threshold);
    }

    public List<Subscription> findExpiredSubscriptions() {
        return subscriptionRepository.findExpired(SubscriptionStatus.ACTIVE, LocalDateTime.now());
    }

    public Optional<Subscription> findSubscriptionByBillingKey(String billingKey) {
        return subscriptionRepository.findByBillingKey(billingKey);
    }

    public void disableAutoRenewal(Subscription subscription) {
        subscription.updateBillingKey(null);
        subscriptionRepository.save(subscription);
    }

    public Subscription createSubscriptionFromPayment(Payment payment, String billingKey) {
        User user = payment.getUser();
        validatePurchasablePlan(payment.getPlan());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(now, payment.getBillingCycle());

        Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(user.getId());
        if (existingSubscription.isPresent()) {
            Subscription subscription = existingSubscription.get();
            if (subscription.isActive()) {
                throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
            }

            subscription.reactivate(
                    payment.getPlan(),
                    payment.getBillingCycle(),
                    now,
                    endDate,
                    payment.getCustomerKey(),
                    billingKey
            );

            subscription = subscriptionRepository.save(subscription);
            payment.linkSubscription(subscription);
            return subscription;
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(payment.getPlan())
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(payment.getBillingCycle())
                .startDate(now)
                .endDate(endDate)
                .customerKey(payment.getCustomerKey())
                .billingKey(billingKey)
                .build();

        subscription = subscriptionRepository.save(subscription);
        payment.linkSubscription(subscription);

        return subscription;
    }

    private void validatePurchasablePlan(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            throw new SubscriptionException(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, BillingCycle billingCycle) {
        return startDate.plusMonths(billingCycle.getMonths());
    }
}
