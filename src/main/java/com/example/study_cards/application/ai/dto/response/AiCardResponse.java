package com.example.study_cards.application.ai.dto.response;

import com.example.study_cards.domain.usercard.entity.UserCard;

public record AiCardResponse(
        Long id,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        String categoryCode,
        boolean aiGenerated
) {
    public static AiCardResponse from(UserCard card) {
        return new AiCardResponse(
                card.getId(),
                card.getQuestion(),
                card.getQuestionSub(),
                card.getAnswer(),
                card.getAnswerSub(),
                card.getCategory().getCode(),
                card.getAiGenerated()
        );
    }
}
