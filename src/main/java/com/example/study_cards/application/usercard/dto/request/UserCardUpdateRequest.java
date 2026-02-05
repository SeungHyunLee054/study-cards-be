package com.example.study_cards.application.usercard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCardUpdateRequest(
        @NotBlank(message = "질문은 필수입니다.")
        String question,

        String questionSub,

        @NotBlank(message = "정답은 필수입니다.")
        String answer,

        String answerSub,

        @NotNull(message = "카테고리는 필수입니다.")
        String category
) {
}
