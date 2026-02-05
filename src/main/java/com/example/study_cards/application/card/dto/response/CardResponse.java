package com.example.study_cards.application.card.dto.response;

import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.usercard.entity.UserCard;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        Double efFactor,
        CategoryResponse category,
        CardType cardType,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getQuestion(),
                card.getQuestionSub(),
                card.getAnswer(),
                card.getAnswerSub(),
                card.getEfFactor(),
                CategoryResponse.from(card.getCategory()),
                CardType.PUBLIC,
                card.getCreatedAt()
        );
    }

    public static CardResponse fromUserCard(UserCard userCard) {
        return new CardResponse(
                userCard.getId(),
                userCard.getQuestion(),
                userCard.getQuestionSub(),
                userCard.getAnswer(),
                userCard.getAnswerSub(),
                userCard.getEfFactor(),
                CategoryResponse.from(userCard.getCategory()),
                CardType.CUSTOM,
                userCard.getCreatedAt()
        );
    }
}
