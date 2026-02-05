package com.example.study_cards.application.generation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApprovalRequest(
        @NotEmpty(message = "승인할 카드 ID 목록은 필수입니다.")
        List<Long> ids
) {
}
