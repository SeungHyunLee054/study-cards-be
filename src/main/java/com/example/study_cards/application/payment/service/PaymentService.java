package com.example.study_cards.application.payment.service;

import com.example.study_cards.application.payment.dto.request.CheckoutRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmBillingRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.payment.dto.response.CheckoutResponse;
import com.example.study_cards.application.payment.dto.response.PaymentHistoryResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.payment.dto.response.TossBillingAuthResponse;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final PaymentDomainService paymentDomainService;
    private final TossPaymentService tossPaymentService;

    @Transactional
    public CheckoutResponse checkout(User user, CheckoutRequest request) {
        if (request.plan() == SubscriptionPlan.FREE) {
            throw new SubscriptionException(SubscriptionErrorCode.FREE_PLAN_NOT_PURCHASABLE);
        }

        if (subscriptionDomainService.hasActiveSubscription(user.getId())) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        String customerKey = generateCustomerKey();
        int amount = request.plan().getPrice(request.billingCycle());

        Payment payment = paymentDomainService.createInitialPayment(
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
    public SubscriptionResponse confirmBilling(User user, ConfirmBillingRequest request) {
        Payment payment = paymentDomainService.getPaymentByOrderIdForUpdate(request.orderId());

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        if (payment.getBillingCycle() != BillingCycle.MONTHLY) {
            throw new PaymentException(PaymentErrorCode.BILLING_NOT_SUPPORTED_FOR_CYCLE);
        }

        if (!Objects.equals(payment.getCustomerKey(), request.customerKey())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CUSTOMER_KEY_MISMATCH);
        }

        if (payment.isCompleted()) {
            return findExistingSubscription(user.getId());
        }

        if (!payment.isPending()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        TossBillingAuthResponse billingAuthResponse = tossPaymentService.issueBillingKey(
                request.authKey(),
                request.customerKey()
        );

        String billingKey = billingAuthResponse.billingKey();

        int amount = payment.getAmount();
        String orderName = String.format("%s %s 구독",
                payment.getPlan().getDisplayName(),
                payment.getBillingCycle().getDisplayName());

        try {
            var billingResponse = tossPaymentService.billingPayment(
                    billingKey,
                    request.customerKey(),
                    payment.getOrderId(),
                    amount,
                    orderName
            );

            paymentDomainService.completePayment(
                    payment,
                    billingResponse.paymentKey(),
                    billingResponse.method()
            );

            Subscription subscription = subscriptionDomainService.createSubscriptionFromPayment(payment, billingKey);

            return SubscriptionResponse.from(subscription);
        } catch (Exception e) {
            log.warn("빌링 결제 실패: orderId={}, billingKey={}, error={}",
                    payment.getOrderId(), billingKey, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public SubscriptionResponse confirmPayment(User user, ConfirmPaymentRequest request) {
        Payment payment = paymentDomainService.getPaymentByOrderIdForUpdate(request.orderId());

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        if (payment.getBillingCycle() != BillingCycle.YEARLY) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_SUPPORTED_FOR_CYCLE);
        }

        if (payment.isCompleted()) {
            return findExistingSubscription(user.getId());
        }

        if (!payment.isPending()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (!payment.getAmount().equals(request.amount())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        var tossResponse = tossPaymentService.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        paymentDomainService.completePayment(
                payment,
                tossResponse.paymentKey(),
                tossResponse.method()
        );

        Subscription subscription = subscriptionDomainService.createSubscriptionFromPayment(payment, null);

        return SubscriptionResponse.from(subscription);
    }

    public Page<PaymentHistoryResponse> getPaymentHistory(User user, Pageable pageable) {
        return paymentDomainService.findCompletedByUserId(user.getId(), pageable)
                .map(PaymentHistoryResponse::from);
    }

    private SubscriptionResponse findExistingSubscription(Long userId) {
        return subscriptionDomainService.findActiveByUserId(userId)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_PROCESSED));
    }

    private String generateCustomerKey() {
        return "CK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
