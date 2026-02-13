package com.example.study_cards.application.ai.controller;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.ai.dto.response.AiLimitResponse;
import com.example.study_cards.application.ai.dto.response.UserAiGenerationResponse;
import com.example.study_cards.application.ai.service.UserAiCardService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiCardController {

    private final UserAiCardService userAiCardService;
    private final UserDomainService userDomainService;

    @PostMapping("/generate-cards")
    public ResponseEntity<UserAiGenerationResponse> generateCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GenerateUserCardRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        UserAiGenerationResponse response = userAiCardService.generateCards(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/generation-limit")
    public ResponseEntity<AiLimitResponse> getGenerationLimit(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        AiLimitResponse response = userAiCardService.getGenerationLimit(user);
        return ResponseEntity.ok(response);
    }
}
