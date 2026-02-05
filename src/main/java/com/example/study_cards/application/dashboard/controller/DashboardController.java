package com.example.study_cards.application.dashboard.controller;

import com.example.study_cards.application.dashboard.dto.response.DashboardResponse;
import com.example.study_cards.application.dashboard.service.DashboardService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserDomainService userDomainService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        DashboardResponse result = dashboardService.getDashboard(user);
        return ResponseEntity.ok(result);
    }
}
