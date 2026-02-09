package com.example.study_cards.application.payment.service;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.repository.PaymentRepository;
import com.example.study_cards.domain.payment.service.PaymentDomainService;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.infra.payment.dto.TossConfirmResponse;
import com.example.study_cards.infra.payment.dto.TossWebhookPayload.DataPayload;
import com.example.study_cards.infra.payment.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PaymentWebhookService {

    private final SubscriptionDomainService subscriptionDomainService;
    private final PaymentDomainService paymentDomainService;
    private final PaymentRepository paymentRepository;
    private final TossPaymentService tossPaymentService;
    private final NotificationService notificationService;

    public void handlePaymentStatusChanged(DataPayload data) {
        String status = data.status();

        switch (status) {
            case "DONE" -> handlePaymentDone(data);
            case "CANCELED" -> handlePaymentCanceled(data);
            case "ABORTED", "EXPIRED" -> handlePaymentFailed(data, status);
            default -> log.info("Unhandled payment status: {}", status);
        }
    }

    public void handleBillingKeyDeleted(DataPayload data) {
        String billingKey = data.billingKey();
        if (billingKey == null) {
            log.warn("BILLING_KEY_DELETED webhook received without billingKey");
            return;
        }

        subscriptionDomainService.findSubscriptionByBillingKey(billingKey)
                .ifPresentOrElse(
                        subscription -> {
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
        paymentRepository.findByOrderId(data.orderId())
                .ifPresentOrElse(
                        payment -> processPaymentDone(payment, data),
                        () -> log.warn("Payment not found for webhook DONE: orderId={}", data.orderId())
                );
    }

    private void processPaymentDone(Payment payment, DataPayload data) {
        boolean completed = paymentDomainService.tryCompletePayment(
                payment, data.paymentKey(), data.method());

        if (!completed) {
            log.info("Payment already processed, skipping: orderId={}", data.orderId());
            return;
        }

        log.info("Payment completed via webhook: orderId={}", data.orderId());

        // Toss API에서 결제 상세 조회하여 billingKey 추출
        String billingKey = null;
        try {
            TossConfirmResponse paymentDetail = tossPaymentService.getPayment(data.paymentKey());
            if (paymentDetail.card() != null && paymentDetail.card().billingKey() != null) {
                billingKey = paymentDetail.card().billingKey();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch payment detail for billingKey: paymentKey={}", data.paymentKey());
        }

        try {
            Subscription subscription = subscriptionDomainService.createSubscriptionFromPayment(payment, billingKey);
            log.info("Subscription created via webhook: userId={}, subscriptionId={}",
                    payment.getUser().getId(), subscription.getId());
        } catch (Exception e) {
            log.info("Subscription already exists for user, skipping: userId={}", payment.getUser().getId());
        }
    }

    private void handlePaymentCanceled(DataPayload data) {
        paymentRepository.findByPaymentKey(data.paymentKey())
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
        paymentRepository.findByOrderId(data.orderId())
                .ifPresentOrElse(
                        payment -> {
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
