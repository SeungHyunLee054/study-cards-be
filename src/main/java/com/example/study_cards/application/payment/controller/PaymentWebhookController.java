package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.payment.service.PaymentWebhookService;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.infra.payment.config.TossPaymentProperties;
import com.example.study_cards.infra.payment.dto.response.TossWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

        if (!verifySignature(rawBody, signature)) {
            log.warn("Invalid webhook signature");
            throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }

        processWebhook(payload);

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
        String webhookSecret = tossPaymentProperties.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Webhook secret is not configured");
            return false;
        }

        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] expectedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return MessageDigest.isEqual(expectedBytes, signatureBytes);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
