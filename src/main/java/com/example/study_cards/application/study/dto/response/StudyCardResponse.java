package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;

public record StudyCardResponse(
        Long id,
        String question,
        String answer,
        Category category
) {
    public static StudyCardResponse from(Card card) {
        return new StudyCardResponse(
                card.getId(),
                card.getQuestion(),
                card.getAnswer(),
                card.getCategory()
        );
    }
}
