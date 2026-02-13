package com.example.study_cards.application.study.dto.request;

import com.example.study_cards.application.card.dto.response.CardType;
import jakarta.validation.constraints.NotNull;

public record StudyAnswerRequest(
        @NotNull(message = "카드 ID는 필수입니다.")
        Long cardId,

        @NotNull(message = "카드 타입은 필수입니다.")
        CardType cardType,

        @NotNull(message = "정답 여부는 필수입니다.")
        Boolean isCorrect
) {
}
