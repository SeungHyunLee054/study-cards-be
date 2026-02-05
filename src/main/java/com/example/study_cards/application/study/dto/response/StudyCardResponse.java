package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.card.entity.Card;

public record StudyCardResponse(
        Long id,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        CategoryResponse category
) {
    public static StudyCardResponse from(Card card) {
        return new StudyCardResponse(
                card.getId(),
                card.getQuestion(),
                card.getQuestionSub(),
                card.getAnswer(),
                card.getAnswerSub(),
                CategoryResponse.from(card.getCategory())
        );
    }
}
