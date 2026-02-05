package com.example.study_cards.application.notification.dto.response;

import com.example.study_cards.domain.user.entity.User;

public record PushSettingResponse(
        Boolean pushEnabled,
        boolean hasFcmToken
) {
    public static PushSettingResponse from(User user) {
        return new PushSettingResponse(
                user.getPushEnabled(),
                user.getFcmToken() != null && !user.getFcmToken().isBlank()
        );
    }
}
