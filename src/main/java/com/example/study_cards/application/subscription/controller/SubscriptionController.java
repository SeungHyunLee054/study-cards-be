package com.example.study_cards.application.subscription.controller;

import com.example.study_cards.application.subscription.dto.request.CancelSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.request.CheckoutRequest;
import com.example.study_cards.application.subscription.dto.request.ConfirmPaymentRequest;
import com.example.study_cards.application.subscription.dto.response.CheckoutResponse;
import com.example.study_cards.application.subscription.dto.response.PaymentHistoryResponse;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.application.subscription.service.SubscriptionService;
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

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserDomainService userDomainService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getPlans());
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        SubscriptionResponse response = subscriptionService.getMySubscriptionOrNull(user);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(subscriptionService.checkout(user, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<SubscriptionResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(subscriptionService.confirmPayment(user, request));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) CancelSubscriptionRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        subscriptionService.cancelSubscription(user, request != null ? request : new CancelSubscriptionRequest(null));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invoices")
    public ResponseEntity<Page<PaymentHistoryResponse>> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = userDomainService.findById(userDetails.userId());
        return ResponseEntity.ok(subscriptionService.getPaymentHistory(user, pageable));
    }
}
