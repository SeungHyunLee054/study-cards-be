package com.example.study_cards.application.usercard.controller;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.application.usercard.service.UserCardService;
import com.example.study_cards.infra.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/cards")
public class UserCardController {

    private final UserCardService userCardService;

    @GetMapping
    public ResponseEntity<Page<UserCardResponse>> getUserCards(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserCardResponse> cards;
        if (category != null && !category.isBlank()) {
            cards = userCardService.getUserCardsByCategory(userDetails.userId(), category, pageable);
        } else {
            cards = userCardService.getUserCards(userDetails.userId(), pageable);
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
    public ResponseEntity<Page<UserCardResponse>> getUserCardsForStudy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "efFactor", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userCardService.getUserCardsForStudy(userDetails.userId(), category, pageable));
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
