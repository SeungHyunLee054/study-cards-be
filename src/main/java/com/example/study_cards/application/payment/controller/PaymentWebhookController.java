package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.payment.service.PaymentWebhookService;
import com.example.study_cards.infra.payment.dto.response.TossWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(@RequestBody String rawBody) {

        TossWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, TossWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info("Received Toss webhook: eventType={}", payload.eventType());

        processWebhook(payload);

        return ResponseEntity.ok().build();
    }

    private void processWebhook(TossWebhookPayload payload) {
        String eventType = payload.eventType();
        String orderId = payload.data() != null ? payload.data().orderId() : null;
        String status = payload.data() != null ? payload.data().status() : null;

        log.info("Processing webhook: eventType={}, orderId={}, status={}", eventType, orderId, status);

        switch (eventType) {
            case "PAYMENT_STATUS_CHANGED" -> {
                if (payload.data() == null) {
                    log.warn("PAYMENT_STATUS_CHANGED webhook received without data");
                    return;
                }
                paymentWebhookService.handlePaymentStatusChanged(payload.data());
            }
            case "BILLING_DELETED" -> {
                TossWebhookPayload.DataPayload data = payload.data();
                if (data == null && payload.billingKey() != null) {
                    data = new TossWebhookPayload.DataPayload(
                            null, null, null, null, null, null, null, payload.reason(), payload.billingKey()
                    );
                }

                if (data == null || data.billingKey() == null) {
                    log.warn("BILLING_DELETED webhook received without billingKey");
                    return;
                }

                paymentWebhookService.handleBillingKeyDeleted(data);
            }
            default -> log.info("Unhandled webhook event: {}", eventType);
        }
    }
}
