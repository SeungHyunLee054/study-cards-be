package com.example.study_cards.application.auth.dto.response;

import com.example.study_cards.domain.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Integer streak,
        Double masteryRate
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStreak(),
                user.getMasteryRate()
        );
    }
}
