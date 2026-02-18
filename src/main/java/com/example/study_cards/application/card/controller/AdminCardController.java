package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.card.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/cards")
public class AdminCardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<Page<CardResponse>> getCards(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CardResponse> cards;
        if (keyword != null && !keyword.isBlank()) {
            cards = cardService.searchCards(null, keyword, category, pageable);
        } else if (category != null && !category.isBlank()) {
            cards = cardService.getCardsByCategory(category, pageable);
        } else {
            cards = cardService.getCards(pageable);
        }
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
