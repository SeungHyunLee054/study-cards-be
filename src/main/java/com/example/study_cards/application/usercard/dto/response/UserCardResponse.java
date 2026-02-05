package com.example.study_cards.application.usercard.dto.response;

import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.usercard.entity.UserCard;

import java.time.LocalDateTime;

public record UserCardResponse(
        Long id,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        Double efFactor,
        CategoryResponse category,
        LocalDateTime createdAt
) {
    public static UserCardResponse from(UserCard userCard) {
        return new UserCardResponse(
                userCard.getId(),
                userCard.getQuestion(),
                userCard.getQuestionSub(),
                userCard.getAnswer(),
                userCard.getAnswerSub(),
                userCard.getEfFactor(),
                CategoryResponse.from(userCard.getCategory()),
                userCard.getCreatedAt()
        );
    }
}
