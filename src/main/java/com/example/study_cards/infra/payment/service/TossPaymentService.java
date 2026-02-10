package com.example.study_cards.infra.payment.service;

import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.infra.payment.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Service
public class TossPaymentService {

    private final RestClient tossPaymentRestClient;

    public TossBillingAuthResponse issueBillingKey(String authKey, String customerKey) {
        TossBillingAuthRequest request = new TossBillingAuthRequest(authKey, customerKey);

        try {
            TossBillingAuthResponse response = tossPaymentRestClient.post()
                    .uri("/billing/authorizations/issue")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Toss billing key issue failed: status={}", res.getStatusCode());
                        throw new PaymentException(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED);
                    })
                    .body(TossBillingAuthResponse.class);

            if (response == null) {
                throw new PaymentException(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED);
            }

            log.info("Billing key issued: customerKey={}", response.customerKey());

            return response;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss billing key issue error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.BILLING_KEY_ISSUE_FAILED);
        }
    }

    public TossConfirmResponse confirmPayment(String paymentKey, String orderId, Integer amount) {
        TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);

        try {
            TossConfirmResponse response = tossPaymentRestClient.post()
                    .uri("/payments/confirm")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Toss payment confirm failed: status={}", res.getStatusCode());
                        throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED);
                    })
                    .body(TossConfirmResponse.class);

            if (response == null) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED);
            }

            log.info("Payment confirmed: orderId={}, paymentKey={}, amount={}",
                    response.orderId(), response.paymentKey(), response.totalAmount());

            return response;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss payment confirm error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_CONFIRMATION_FAILED);
        }
    }

    public TossConfirmResponse billingPayment(String billingKey, String customerKey, String orderId,
                                               Integer amount, String orderName) {
        TossBillingRequest request = new TossBillingRequest(amount, customerKey, orderId, orderName);

        try {
            TossConfirmResponse response = tossPaymentRestClient.post()
                    .uri("/billing/{billingKey}", billingKey)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Toss billing payment failed: status={}", res.getStatusCode());
                        throw new PaymentException(PaymentErrorCode.BILLING_PAYMENT_FAILED);
                    })
                    .body(TossConfirmResponse.class);

            if (response == null) {
                throw new PaymentException(PaymentErrorCode.BILLING_PAYMENT_FAILED);
            }

            log.info("Billing payment completed: orderId={}, paymentKey={}, amount={}",
                    response.orderId(), response.paymentKey(), response.totalAmount());

            return response;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss billing payment error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.BILLING_PAYMENT_FAILED);
        }
    }

    public void cancelPayment(String paymentKey, String cancelReason) {
        try {
            tossPaymentRestClient.post()
                    .uri("/payments/{paymentKey}/cancel", paymentKey)
                    .body(new CancelRequest(cancelReason))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Toss payment cancel failed: status={}", res.getStatusCode());
                        throw new PaymentException(PaymentErrorCode.PAYMENT_CANCEL_FAILED);
                    })
                    .toBodilessEntity();

            log.info("Payment canceled: paymentKey={}, reason={}", paymentKey, cancelReason);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss payment cancel error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    public TossConfirmResponse getPayment(String paymentKey) {
        try {
            return tossPaymentRestClient.get()
                    .uri("/payments/{paymentKey}", paymentKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Toss get payment failed: status={}", res.getStatusCode());
                        throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                    })
                    .body(TossConfirmResponse.class);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Toss get payment error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    private record CancelRequest(String cancelReason) {
    }
}
