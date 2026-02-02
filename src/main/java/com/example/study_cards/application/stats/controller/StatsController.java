package com.example.study_cards.application.stats.controller;

import com.example.study_cards.application.stats.dto.response.StatsResponse;
import com.example.study_cards.application.stats.service.StatsService;
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
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final UserDomainService userDomainService;

    @GetMapping
    public ResponseEntity<StatsResponse> getStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        StatsResponse stats = statsService.getStats(user);
        return ResponseEntity.ok(stats);
    }
}
