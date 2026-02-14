package com.example.study_cards.application.subscription.controller;

import com.example.study_cards.application.subscription.dto.request.CancelSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.request.ResumeSubscriptionRequest;
import com.example.study_cards.application.subscription.dto.response.PlanResponse;
import com.example.study_cards.application.subscription.dto.response.ResumeSubscriptionPrepareResponse;
import com.example.study_cards.application.subscription.dto.response.SubscriptionResponse;
import com.example.study_cards.application.subscription.service.SubscriptionService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) CancelSubscriptionRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        SubscriptionResponse response = subscriptionService.cancelSubscription(
                user,
                request != null ? request : new CancelSubscriptionRequest(null)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/resume", "/reactivate"})
    public ResponseEntity<SubscriptionResponse> resumeSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) ResumeSubscriptionRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        SubscriptionResponse response = subscriptionService.resumeSubscription(
                user,
                request != null ? request : new ResumeSubscriptionRequest(null)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resume/prepare")
    public ResponseEntity<ResumeSubscriptionPrepareResponse> prepareResumeSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        ResumeSubscriptionPrepareResponse response = subscriptionService.prepareResumeSubscription(user);
        return ResponseEntity.ok(response);
    }
}
