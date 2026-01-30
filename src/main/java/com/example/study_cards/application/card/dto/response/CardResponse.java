package com.example.study_cards.application.card.dto.response;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String question,
        String answer,
        Double efFactor,
        Category category,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getQuestion(),
                card.getAnswer(),
                card.getEfFactor(),
                card.getCategory(),
                card.getCreatedAt()
        );
    }
}
