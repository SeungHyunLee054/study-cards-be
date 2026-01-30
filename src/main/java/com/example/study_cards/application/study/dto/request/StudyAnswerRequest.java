package com.example.study_cards.application.study.dto.request;

import jakarta.validation.constraints.NotNull;

public record StudyAnswerRequest(
        @NotNull(message = "카드 ID는 필수입니다.")
        Long cardId,

        @NotNull(message = "정답 여부는 필수입니다.")
        Boolean isCorrect
) {
}
