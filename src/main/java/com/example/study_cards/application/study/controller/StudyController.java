package com.example.study_cards.application.study.controller;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.*;
import com.example.study_cards.application.study.service.StudyAiRecommendationService;
import com.example.study_cards.application.study.service.StudyRecommendationService;
import com.example.study_cards.application.study.service.StudyService;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;
    private final StudyRecommendationService studyRecommendationService;
    private final StudyAiRecommendationService studyAiRecommendationService;
    private final UserDomainService userDomainService;

    @GetMapping("/cards")
    public ResponseEntity<Page<StudyCardResponse>> getTodayCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "efFactor", direction = Sort.Direction.ASC) Pageable pageable) {
        User user = userDomainService.findById(userDetails.userId());
        Page<StudyCardResponse> cards = studyService.getTodayCards(user, category, pageable);
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

    @PutMapping("/sessions/end")
    public ResponseEntity<SessionResponse> endCurrentSession(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        SessionResponse result = studyService.endCurrentSession(user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/current")
    public ResponseEntity<SessionResponse> getCurrentSession(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        SessionResponse result = studyService.getCurrentSession(user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = userDomainService.findById(userDetails.userId());
        SessionResponse result = studyService.getSession(user, sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions")
    public ResponseEntity<Page<SessionResponse>> getSessionHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userDomainService.findById(userDetails.userId());
        Page<SessionResponse> result = studyService.getSessionHistory(user, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/{sessionId}/stats")
    public ResponseEntity<SessionStatsResponse> getSessionStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        User user = userDomainService.findById(userDetails.userId());
        SessionStatsResponse result = studyService.getSessionStats(user, sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        User user = userDomainService.findById(userDetails.userId());
        RecommendationResponse result = studyRecommendationService.getRecommendations(user, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recommendations/ai")
    public ResponseEntity<AiRecommendationResponse> getAiRecommendations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        User user = userDomainService.findById(userDetails.userId());
        AiRecommendationResponse result = studyAiRecommendationService.getAiRecommendations(user, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/category-accuracy")
    public ResponseEntity<List<CategoryAccuracyResponse>> getCategoryAccuracy(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDomainService.findById(userDetails.userId());
        List<CategoryAccuracyResponse> result = studyRecommendationService.getCategoryAccuracy(user);
        return ResponseEntity.ok(result);
    }
}
