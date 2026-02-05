package com.example.study_cards.application.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record PushSettingRequest(
        @NotNull(message = "푸시 설정 값은 필수입니다.")
        Boolean pushEnabled
) {
}
