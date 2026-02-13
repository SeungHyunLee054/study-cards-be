package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.application.card.dto.response.CardType;
import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.usercard.entity.UserCard;

public record StudyCardResponse(
        Long id,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        CategoryResponse category,
        CardType cardType
) {
    public static StudyCardResponse from(Card card) {
        return new StudyCardResponse(
                card.getId(),
                card.getQuestion(),
                card.getQuestionSub(),
                card.getAnswer(),
                card.getAnswerSub(),
                CategoryResponse.from(card.getCategory()),
                CardType.PUBLIC
        );
    }

    public static StudyCardResponse fromUserCard(UserCard userCard) {
        return new StudyCardResponse(
                userCard.getId(),
                userCard.getQuestion(),
                userCard.getQuestionSub(),
                userCard.getAnswer(),
                userCard.getAnswerSub(),
                CategoryResponse.from(userCard.getCategory()),
                CardType.CUSTOM
        );
    }
}
