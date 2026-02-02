package com.example.study_cards.application.usercard.dto.request;

import com.example.study_cards.domain.card.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCardUpdateRequest(
        @NotBlank(message = "영문 질문은 필수입니다.")
        String questionEn,

        String questionKo,

        @NotBlank(message = "영문 정답은 필수입니다.")
        String answerEn,

        String answerKo,

        @NotNull(message = "카테고리는 필수입니다.")
        Category category
) {
}
