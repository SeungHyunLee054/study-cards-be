package com.example.study_cards.application.notification.controller;

import com.example.study_cards.application.notification.dto.request.FcmTokenRequest;
import com.example.study_cards.application.notification.dto.request.PushSettingRequest;
import com.example.study_cards.application.notification.dto.response.NotificationResponse;
import com.example.study_cards.application.notification.dto.response.PushSettingResponse;
import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request) {
        notificationService.registerFcmToken(userDetails.userId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<Void> removeFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.removeFcmToken(userDetails.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<PushSettingResponse> getPushSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getPushSettings(userDetails.userId()));
    }

    @PatchMapping("/settings")
    public ResponseEntity<PushSettingResponse> updatePushSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PushSettingRequest request) {
        return ResponseEntity.ok(notificationService.updatePushSettings(userDetails.userId(), request));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.userId(), pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userDetails.userId(), pageable));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userDetails.userId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        notificationService.markAsRead(userDetails.userId(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.userId());
        return ResponseEntity.ok().build();
    }
}
