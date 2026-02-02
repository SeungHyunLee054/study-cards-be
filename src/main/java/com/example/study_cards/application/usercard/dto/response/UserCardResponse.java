package com.example.study_cards.application.usercard.dto.response;

import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.usercard.entity.UserCard;

import java.time.LocalDateTime;

public record UserCardResponse(
        Long id,
        String questionEn,
        String questionKo,
        String answerEn,
        String answerKo,
        Double efFactor,
        Category category,
        LocalDateTime createdAt
) {
    public static UserCardResponse from(UserCard userCard) {
        return new UserCardResponse(
                userCard.getId(),
                userCard.getQuestionEn(),
                userCard.getQuestionKo(),
                userCard.getAnswerEn(),
                userCard.getAnswerKo(),
                userCard.getEfFactor(),
                userCard.getCategory(),
                userCard.getCreatedAt()
        );
    }
}
