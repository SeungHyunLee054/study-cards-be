package com.example.study_cards.application.ai.controller;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.ai.dto.response.AiLimitResponse;
import com.example.study_cards.application.ai.dto.response.UserAiGenerationResponse;
import com.example.study_cards.application.ai.service.AiSourceTextExtractorService;
import com.example.study_cards.application.ai.service.UserAiCardService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AiCardController {

    private final UserAiCardService userAiCardService;
    private final AiSourceTextExtractorService aiSourceTextExtractorService;
    private final UserDomainService userDomainService;

    @PostMapping("/generate-cards")
    public ResponseEntity<UserAiGenerationResponse> generateCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GenerateUserCardRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        UserAiGenerationResponse response = userAiCardService.generateCards(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/generate-cards/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserAiGenerationResponse> generateCardsByUpload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotBlank String categoryCode,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) Integer count,
            @RequestParam(required = false) String difficulty) {
        User user = userDomainService.findById(userDetails.userId());
        String sourceText = aiSourceTextExtractorService.extractText(file);
        GenerateUserCardRequest request = new GenerateUserCardRequest(sourceText, categoryCode, count, difficulty);
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
