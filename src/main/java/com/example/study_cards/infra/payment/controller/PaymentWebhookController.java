package com.example.study_cards.infra.payment.controller;

import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.subscription.entity.Payment;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.exception.SubscriptionErrorCode;
import com.example.study_cards.domain.subscription.exception.SubscriptionException;
import com.example.study_cards.domain.subscription.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.infra.payment.config.TossPaymentProperties;
import com.example.study_cards.infra.payment.dto.TossWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final TossPaymentProperties tossPaymentProperties;
    private final SubscriptionDomainService subscriptionDomainService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String rawBody,
            @RequestBody TossWebhookPayload payload) {

        log.info("Received Toss webhook: eventType={}", payload.eventType());

        if (tossPaymentProperties.getWebhookSecret() != null && !tossPaymentProperties.getWebhookSecret().isBlank()) {
            if (!verifySignature(rawBody, signature)) {
                log.warn("Invalid webhook signature");
                throw new SubscriptionException(SubscriptionErrorCode.INVALID_WEBHOOK_SIGNATURE);
            }
        }

        try {
            processWebhook(payload);
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private void processWebhook(TossWebhookPayload payload) {
        if (payload.data() == null) {
            log.warn("Webhook data is null");
            return;
        }

        String eventType = payload.eventType();
        String paymentKey = payload.data().paymentKey();
        String orderId = payload.data().orderId();
        String status = payload.data().status();

        log.info("Processing webhook: eventType={}, orderId={}, status={}", eventType, orderId, status);

        switch (eventType) {
            case "PAYMENT_STATUS_CHANGED" -> handlePaymentStatusChanged(payload.data());
            case "BILLING_KEY_DELETED" -> handleBillingKeyDeleted(payload.data());
            default -> log.info("Unhandled webhook event: {}", eventType);
        }
    }

    private void handlePaymentStatusChanged(TossWebhookPayload.DataPayload data) {
        String status = data.status();

        switch (status) {
            case "DONE" -> {
                log.info("Payment completed via webhook: orderId={}", data.orderId());
            }
            case "CANCELED" -> {
                paymentRepository.findByPaymentKey(data.paymentKey())
                        .ifPresent(payment -> {
                            subscriptionDomainService.cancelPayment(payment, data.cancelReason());
                            log.info("Payment canceled via webhook: orderId={}", data.orderId());
                        });
            }
            case "ABORTED", "EXPIRED" -> {
                paymentRepository.findByOrderId(data.orderId())
                        .ifPresent(payment -> {
                            subscriptionDomainService.failPayment(payment, "Payment " + status.toLowerCase());
                            log.info("Payment {} via webhook: orderId={}", status.toLowerCase(), data.orderId());

                            if (payment.getUser() != null) {
                                notificationService.sendNotification(
                                        payment.getUser(),
                                        NotificationType.PAYMENT_FAILED,
                                        "결제 실패",
                                        "결제가 실패했습니다. 결제 수단을 확인해주세요."
                                );
                            }
                        });
            }
            default -> log.info("Unhandled payment status: {}", status);
        }
    }

    private void handleBillingKeyDeleted(TossWebhookPayload.DataPayload data) {
        log.info("Billing key deleted: {}", data.paymentKey());
    }

    private boolean verifySignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    tossPaymentProperties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
