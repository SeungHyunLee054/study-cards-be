package com.example.study_cards.application.usercard.controller;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.application.usercard.service.UserCardService;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/cards")
public class UserCardController {

    private final UserCardService userCardService;

    @GetMapping
    public ResponseEntity<List<UserCardResponse>> getUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category) {
        List<UserCardResponse> cards;
        if (category != null) {
            cards = userCardService.getUserCardsByCategory(userDetails.userId(), category);
        } else {
            cards = userCardService.getUserCards(userDetails.userId());
        }
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserCardResponse> getUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(userCardService.getUserCard(userDetails.userId(), id));
    }

    @GetMapping("/study")
    public ResponseEntity<List<UserCardResponse>> getUserCardsForStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(userCardService.getUserCardsForStudy(userDetails.userId(), category));
    }

    @PostMapping
    public ResponseEntity<UserCardResponse> createUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserCardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userCardService.createUserCard(userDetails.userId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserCardResponse> updateUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UserCardUpdateRequest request) {
        return ResponseEntity.ok(userCardService.updateUserCard(userDetails.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserCard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        userCardService.deleteUserCard(userDetails.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
