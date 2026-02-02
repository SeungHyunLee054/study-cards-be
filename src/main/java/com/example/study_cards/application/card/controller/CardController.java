package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.card.service.CardService;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(@RequestParam(required = false) Category category) {
        List<CardResponse> cards;
        if (category != null) {
            cards = cardService.getCardsByCategory(category);
        } else {
            cards = cardService.getCards();
        }
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @GetMapping("/study")
    public ResponseEntity<List<CardResponse>> getCardsForStudy(
            @RequestParam(required = false) Category category,
            Authentication authentication,
            HttpServletRequest request) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String ipAddress = getClientIpAddress(request);
        return ResponseEntity.ok(cardService.getCardsForStudy(category, isAuthenticated, ipAddress));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CardResponse>> getAllCardsWithUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(cardService.getAllCardsWithUserCards(userDetails.userId(), category));
    }

    @GetMapping("/study/all")
    public ResponseEntity<List<CardResponse>> getCardsForStudyWithUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(cardService.getCardsForStudyWithUserCards(userDetails.userId(), category));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCardCount(@RequestParam(required = false) Category category) {
        return ResponseEntity.ok(cardService.getCardCount(category));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
