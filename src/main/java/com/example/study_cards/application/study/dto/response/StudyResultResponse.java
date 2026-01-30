package com.example.study_cards.application.study.dto.response;

import java.time.LocalDate;

public record StudyResultResponse(
        Long cardId,
        Boolean isCorrect,
        LocalDate nextReviewDate,
        Double newEfFactor
) {
}
