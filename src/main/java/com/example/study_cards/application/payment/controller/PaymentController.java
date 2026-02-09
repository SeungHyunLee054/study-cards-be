package com.example.study_cards.application.payment.controller;

import com.example.study_cards.application.payment.dto.request.CheckoutRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmBillingRequest;
import com.example.study_cards.application.payment.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.payment.dto.response.CheckoutResponse;
import com.example.study_cards.application.payment.dto.response.PaymentHistoryResponse;
import com.example.study_cards.application.payment.service.PaymentService;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserDomainService userDomainService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(paymentService.checkout(user, request));
    }

    @PostMapping("/confirm-billing")
    public ResponseEntity<SubscriptionResponse> confirmBilling(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConfirmBillingRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(paymentService.confirmBilling(user, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<SubscriptionResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(paymentService.confirmPayment(user, request));
    }

    @GetMapping("/invoices")
    public ResponseEntity<Page<PaymentHistoryResponse>> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(paymentService.getPaymentHistory(user, pageable));
    }
}
