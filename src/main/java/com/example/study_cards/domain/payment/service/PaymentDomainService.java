package com.example.study_cards.domain.payment.service;

import com.example.study_cards.domain.payment.entity.Payment;
import com.example.study_cards.domain.payment.entity.PaymentStatus;
import com.example.study_cards.domain.payment.entity.PaymentType;
import com.example.study_cards.domain.payment.exception.PaymentErrorCode;
import com.example.study_cards.domain.payment.exception.PaymentException;
import com.example.study_cards.domain.payment.repository.PaymentRepository;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;

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

    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment getPaymentByOrderIdForUpdate(String orderId) {
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public Payment getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    public void completePayment(Payment payment, String paymentKey, String method) {
        if (payment.isCompleted()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        payment.complete(paymentKey, method, LocalDateTime.now());
        paymentRepository.save(payment);
    }

    public boolean tryCompletePayment(Payment payment, String paymentKey, String method) {
        if (!payment.isPending()) {
            return false;
        }
        payment.complete(paymentKey, method, LocalDateTime.now());
        paymentRepository.save(payment);
        return true;
    }

    public void cancelPayment(Payment payment, String reason) {
        payment.cancel(reason);
        paymentRepository.save(payment);
    }

    public void failPayment(Payment payment, String reason) {
        payment.fail(reason);
        paymentRepository.save(payment);
    }

    private String generateOrderId() {
        return "ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
