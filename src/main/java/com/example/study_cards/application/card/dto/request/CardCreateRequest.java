package com.example.study_cards.application.card.dto.request;

import com.example.study_cards.domain.card.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardCreateRequest(
        @NotBlank(message = "질문은 필수입니다.")
        String question,

        @NotBlank(message = "정답은 필수입니다.")
        String answer,

        @NotNull(message = "카테고리는 필수입니다.")
        Category category
) {
}
