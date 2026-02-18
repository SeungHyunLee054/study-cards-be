package com.example.study_cards.application.payment.service;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.infra.payment.dto.response.TossConfirmResponse;
import com.example.study_cards.infra.payment.dto.response.TossWebhookPayload.DataPayload;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class PaymentWebhookService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final PaymentDomainService paymentDomainService;
    private final TossPaymentService tossPaymentService;
    private final NotificationService notificationService;

    @Transactional
    public void handlePaymentStatusChanged(DataPayload data) {
        String status = data.status();

        switch (status) {
            case "DONE" -> handlePaymentDone(data);
            case "CANCELED" -> handlePaymentCanceled(data);
            case "ABORTED", "EXPIRED" -> handlePaymentFailed(data, status);
            default -> log.info("Unhandled payment status: {}", status);
        }
    }

    @Transactional
    public void handleBillingKeyDeleted(DataPayload data) {
        String billingKey = data.billingKey();
        if (billingKey == null) {
            log.warn("BILLING_DELETED webhook received without billingKey");
            return;
        }

        subscriptionDomainService.findSubscriptionByBillingKey(billingKey)
                .ifPresentOrElse(
                        subscription -> {
                            if (subscription.getBillingCycle() != BillingCycle.MONTHLY) {
                                subscriptionDomainService.updateBillingKey(subscription, null);
                                log.info("Billing key removed for non-monthly subscription: billingKey={}, userId={}",
                                        billingKey, subscription.getUser().getId());
                                return;
                            }

                            subscription.updateBillingKey(null);
                            subscriptionDomainService.disableAutoRenewal(subscription);
                            log.info("Auto-renewal disabled: billingKey={}, userId={}",
                                    billingKey, subscription.getUser().getId());

                            notificationService.sendNotification(
                                    subscription.getUser(),
                                    NotificationType.AUTO_RENEWAL_DISABLED,
                                    "자동 갱신 해제",
                                    "등록된 결제 수단이 삭제되어 자동 갱신이 해제되었습니다."
                            );
                        },
                        () -> log.warn("No subscription found for billingKey={}", billingKey)
                );
    }

    private void handlePaymentDone(DataPayload data) {
        paymentDomainService.findByOrderIdForUpdateOptional(data.orderId())
                .ifPresentOrElse(
                        payment -> processPaymentDone(payment, data),
                        () -> log.warn("Payment not found for webhook DONE: orderId={}", data.orderId())
                );
    }

    private void processPaymentDone(Payment payment, DataPayload data) {
        if (data.paymentKey() == null || data.paymentKey().isBlank()) {
            log.warn("Webhook DONE 이벤트에 paymentKey가 없습니다: orderId={}", data.orderId());
            return;
        }

        TossConfirmResponse paymentDetail;
        try {
            paymentDetail = tossPaymentService.getPayment(data.paymentKey());
        } catch (Exception e) {
            log.warn("Failed to verify webhook payment with Toss API: paymentKey={}, orderId={}",
                    data.paymentKey(), data.orderId());
            return;
        }

        if (!"DONE".equals(paymentDetail.status())) {
            log.warn("Webhook verification failed: payment is not DONE. paymentKey={}, status={}",
                    data.paymentKey(), paymentDetail.status());
            return;
        }

        if (paymentDetail.orderId() == null || !payment.getOrderId().equals(paymentDetail.orderId())) {
            log.warn("Webhook orderId mismatch: expected={}, received={}",
                    payment.getOrderId(), paymentDetail.orderId());
            return;
        }

        if (paymentDetail.totalAmount() == null || !payment.getAmount().equals(paymentDetail.totalAmount())) {
            log.warn("Webhook amount mismatch after Toss verification: orderId={}, expected={}, received={}",
                    payment.getOrderId(), payment.getAmount(), paymentDetail.totalAmount());
            return;
        }

        boolean completed = paymentDomainService.tryCompletePayment(
                payment, paymentDetail.paymentKey(), paymentDetail.method());

        if (!completed) {
            log.info("Payment already processed, skipping: orderId={}", data.orderId());
            return;
        }

        log.info("Payment completed via webhook: orderId={}", data.orderId());

        String billingKey = null;
        if (payment.getBillingCycle() == BillingCycle.MONTHLY
                && paymentDetail.card() != null
                && paymentDetail.card().billingKey() != null) {
            billingKey = paymentDetail.card().billingKey();
        }

        try {
            Subscription subscription = subscriptionDomainService.createSubscriptionFromPayment(payment, billingKey);
            log.info("Subscription created via webhook: userId={}, subscriptionId={}",
                    payment.getUser().getId(), subscription.getId());
        } catch (SubscriptionException e) {
            log.info("Subscription already exists for user, skipping: userId={}", payment.getUser().getId());
        }
    }

    private void handlePaymentCanceled(DataPayload data) {
        paymentDomainService.findByPaymentKeyOptional(data.paymentKey())
                .ifPresentOrElse(
                        payment -> {
                            paymentDomainService.cancelPayment(payment, data.cancelReason());
                            log.info("Payment canceled via webhook: orderId={}", data.orderId());

                            Subscription subscription = payment.getSubscription();
                            if (subscription != null && subscription.isActive()) {
                                subscriptionDomainService.cancelSubscription(subscription, data.cancelReason());
                                log.info("Subscription canceled due to payment cancellation: userId={}",
                                        payment.getUser().getId());
                            }

                            if (payment.getUser() != null) {
                                notificationService.sendNotification(
                                        payment.getUser(),
                                        NotificationType.PAYMENT_CANCELED,
                                        "결제 취소",
                                        "결제가 취소되었습니다."
                                );
                            }
                        },
                        () -> log.warn("Payment not found for webhook CANCELED: paymentKey={}", data.paymentKey())
                );
    }

    private void handlePaymentFailed(DataPayload data, String status) {
        paymentDomainService.findByOrderIdOptional(data.orderId())
                .ifPresentOrElse(
                        payment -> {
                            if (!payment.isPending()) {
                                log.info("Payment already processed, skipping failed webhook: orderId={}, status={}",
                                        data.orderId(), status);
                                return;
                            }

                            paymentDomainService.failPayment(payment, "Payment " + status.toLowerCase());
                            log.info("Payment {} via webhook: orderId={}", status.toLowerCase(), data.orderId());

                            if (payment.getUser() != null) {
                                notificationService.sendNotification(
                                        payment.getUser(),
                                        NotificationType.PAYMENT_FAILED,
                                        "결제 실패",
                                        "결제가 실패했습니다. 결제 수단을 확인해주세요."
                                );
                            }
                        },
                        () -> log.warn("Payment not found for webhook {}: orderId={}", status, data.orderId())
                );
    }
}
