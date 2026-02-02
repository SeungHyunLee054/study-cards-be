package com.example.study_cards.application.card.dto.response;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.usercard.entity.UserCard;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String questionEn,
        String questionKo,
        String answerEn,
        String answerKo,
        Double efFactor,
        Category category,
        CardType cardType,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getQuestionEn(),
                card.getQuestionKo(),
                card.getAnswerEn(),
                card.getAnswerKo(),
                card.getEfFactor(),
                card.getCategory(),
                CardType.PUBLIC,
                card.getCreatedAt()
        );
    }

    public static CardResponse fromUserCard(UserCard userCard) {
        return new CardResponse(
                userCard.getId(),
                userCard.getQuestionEn(),
                userCard.getQuestionKo(),
                userCard.getAnswerEn(),
                userCard.getAnswerKo(),
                userCard.getEfFactor(),
                userCard.getCategory(),
                CardType.CUSTOM,
                userCard.getCreatedAt()
        );
    }
}
