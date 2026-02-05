package com.example.study_cards.application.subscription.service;

import com.example.study_cards.application.subscription.dto.request.CancelSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.request.CheckoutRequest;
import com.example.study_cards.application.subscription.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.subscription.dto.response.CheckoutResponse;
import com.example.study_cards.application.subscription.dto.response.PaymentHistoryResponse;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.subscription.entity.*;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.repository.SubscriptionRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
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
        return subscriptionRepository.findByUserId(user.getId())
                .map(SubscriptionResponse::from)
                .orElse(null);
    }

    @Transactional
    public CheckoutResponse checkout(User user, CheckoutRequest request) {
        if (request.plan() == SubscriptionPlan.FREE || request.plan() == SubscriptionPlan.BASIC) {
            throw new SubscriptionException(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }

        if (subscriptionDomainService.hasActiveSubscription(user.getId())) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        String customerKey = generateCustomerKey();
        int amount = request.plan().getPrice(request.billingCycle());

        Payment payment = subscriptionDomainService.createInitialPayment(
                user,
                request.plan(),
                request.billingCycle(),
                customerKey,
                amount
        );

        String orderName = String.format("%s %s 구독",
                request.plan().getDisplayName(),
                request.billingCycle().getDisplayName());

        return new CheckoutResponse(
                payment.getOrderId(),
                customerKey,
                amount,
                orderName
        );
    }

    @Transactional
    public SubscriptionResponse confirmPayment(User user, ConfirmPaymentRequest request) {
        Payment payment = subscriptionDomainService.getPaymentByOrderId(request.orderId());

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_NOT_FOUND);
        }

        if (!payment.isPending()) {
            throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (!payment.getAmount().equals(request.amount())) {
            throw new SubscriptionException(SubscriptionErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        var tossResponse = tossPaymentService.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        subscriptionDomainService.completePayment(
                payment,
                tossResponse.paymentKey(),
                tossResponse.method()
        );

        String billingKey = null;
        if (tossResponse.card() != null && tossResponse.card().billingKey() != null) {
            billingKey = tossResponse.card().billingKey();
        }

        Subscription subscription = subscriptionDomainService.createSubscriptionFromPayment(payment, billingKey);

        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void cancelSubscription(User user, CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionDomainService.getSubscription(user.getId());

        if (!subscription.isActive()) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_ACTIVE);
        }

        subscriptionDomainService.cancelSubscription(subscription);
    }

    public Page<PaymentHistoryResponse> getPaymentHistory(User user, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(PaymentHistoryResponse::from);
    }

    private String generateCustomerKey() {
        return "CK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
