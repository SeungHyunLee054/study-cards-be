package com.example.study_cards.domain.subscription.service;

import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SubscriptionDomainService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public Subscription createSubscription(User user, SubscriptionPlan plan, BillingCycle billingCycle, String customerKey) {
        if (subscriptionRepository.existsByUserId(user.getId())) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        validatePurchasablePlan(plan);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(now, billingCycle);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.PENDING)
                .billingCycle(billingCycle)
                .startDate(now)
                .endDate(endDate)
                .customerKey(customerKey)
                .build();

        return subscriptionRepository.save(subscription);
    }

    public Subscription getSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    public Subscription getSubscriptionByCustomerKey(String customerKey) {
        return subscriptionRepository.findByCustomerKey(customerKey)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    public boolean hasActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(Subscription::isActive)
                .orElse(false);
    }

    public void activateSubscription(Subscription subscription, String billingKey) {
        subscription.activate();
        subscription.updateBillingKey(billingKey);
        subscriptionRepository.save(subscription);
    }

    public void cancelSubscription(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED);
        }
        subscription.cancel();
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

    public Payment createPayment(User user, Subscription subscription, int amount, PaymentType type) {
        String orderId = generateOrderId();

        Payment payment = Payment.builder()
                .user(user)
                .subscription(subscription)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .type(type)
                .build();

        return paymentRepository.save(payment);
    }

    public Payment createInitialPayment(User user, SubscriptionPlan plan, BillingCycle billingCycle,
                                        String customerKey, int amount) {
        String orderId = generateOrderId();

        Payment payment = Payment.builder()
                .user(user)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .type(PaymentType.INITIAL)
                .plan(plan)
                .billingCycle(billingCycle)
                .customerKey(customerKey)
                .build();

        return paymentRepository.save(payment);
    }

    public Subscription createSubscriptionFromPayment(Payment payment, String billingKey) {
        User user = payment.getUser();

        if (subscriptionRepository.existsByUserId(user.getId())) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        validatePurchasablePlan(payment.getPlan());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = calculateEndDate(now, payment.getBillingCycle());

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
        paymentRepository.save(payment);

        return subscription;
    }

    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PAYMENT_NOT_FOUND));
    }

    public void completePayment(Payment payment, String paymentKey, String method) {
        if (payment.isCompleted()) {
            throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        payment.complete(paymentKey, method, LocalDateTime.now());
        paymentRepository.save(payment);
    }

    public void cancelPayment(Payment payment, String reason) {
        payment.cancel(reason);
        paymentRepository.save(payment);
    }

    public void failPayment(Payment payment, String reason) {
        payment.fail(reason);
        paymentRepository.save(payment);
    }

    private void validatePurchasablePlan(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE) {
            throw new SubscriptionException(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }
        if (plan == SubscriptionPlan.BASIC) {
            throw new SubscriptionException(SubscriptionErrorCode.BASIC_PLAN_NOT_PURCHASABLE);
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, BillingCycle billingCycle) {
        return startDate.plusMonths(billingCycle.getMonths());
    }

    private String generateOrderId() {
        return "ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
