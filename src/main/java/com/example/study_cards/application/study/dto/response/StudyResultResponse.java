package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.application.card.dto.response.CardType;

import java.time.LocalDate;

public record StudyResultResponse(
        Long cardId,
        CardType cardType,
        Boolean isCorrect,
        LocalDate nextReviewDate,
        Double newEfFactor
) {
}
