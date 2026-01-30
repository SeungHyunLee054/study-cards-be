package com.example.study_cards.application.card.controller;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.card.service.CardService;
import com.example.study_cards.domain.card.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(@RequestParam(required = false) Category category) {
        // TODO
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
        // TODO
        return null;
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@RequestBody CardCreateRequest request) {
        // TODO
        return null;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        // TODO
        return null;
    }
}
