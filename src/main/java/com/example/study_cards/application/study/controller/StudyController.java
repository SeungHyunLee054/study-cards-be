package com.example.study_cards.application.study.controller;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.application.study.service.StudyService;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;
    private final UserDomainService userDomainService;

    @GetMapping("/cards")
    public ResponseEntity<List<StudyCardResponse>> getTodayCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "CS") Category category) {
        User user = userDomainService.findById(userDetails.userId());
        List<StudyCardResponse> cards = studyService.getTodayCards(user, category);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/answer")
    public ResponseEntity<StudyResultResponse> submitAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudyAnswerRequest request) {
        User user = userDomainService.findById(userDetails.userId());
        StudyResultResponse result = studyService.submitAnswer(user, request);
        return ResponseEntity.ok(result);
    }
}
