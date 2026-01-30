package com.example.study_cards.application.card.service;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.service.CardDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CardService {

    private final CardDomainService cardDomainService;

    @Transactional(readOnly = true)
    public List<CardResponse> getCards() {
        // TODO: cardDomainService 호출
        return null;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getCardsByCategory(Category category) {
        // TODO: cardDomainService 호출
        return null;
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long id) {
        // TODO: cardDomainService 호출
        return null;
    }

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        // TODO: cardDomainService 호출
        return null;
    }

    @Transactional
    public void deleteCard(Long id) {
        // TODO: cardDomainService 호출
    }
}
