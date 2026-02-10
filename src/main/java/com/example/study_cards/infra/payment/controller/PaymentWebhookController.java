package com.example.study_cards.infra.payment.controller;

import com.example.study_cards.application.payment.service.PaymentWebhookService;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.infra.payment.config.TossPaymentProperties;
import com.example.study_cards.infra.payment.dto.TossWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "Toss-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        TossWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, TossWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info("Received Toss webhook: eventType={}", payload.eventType());

        if (tossPaymentProperties.getWebhookSecret() != null && !tossPaymentProperties.getWebhookSecret().isBlank()) {
            if (!verifySignature(rawBody, signature)) {
                log.warn("Invalid webhook signature");
                throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_SIGNATURE);
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
        String orderId = payload.data().orderId();
        String status = payload.data().status();

        log.info("Processing webhook: eventType={}, orderId={}, status={}", eventType, orderId, status);

        switch (eventType) {
            case "PAYMENT_STATUS_CHANGED" -> paymentWebhookService.handlePaymentStatusChanged(payload.data());
            case "BILLING_KEY_DELETED" -> paymentWebhookService.handleBillingKeyDeleted(payload.data());
            default -> log.info("Unhandled webhook event: {}", eventType);
        }
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
