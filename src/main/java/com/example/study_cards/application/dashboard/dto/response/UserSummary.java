package com.example.study_cards.application.dashboard.dto.response;

import com.example.study_cards.domain.user.entity.User;

public record UserSummary(
        Long id,
        String nickname,
        Integer streak,
        Integer level,
        Integer totalStudied
) {

    public static UserSummary from(User user, int totalStudied) {
        int level = calculateLevel(totalStudied);

        return new UserSummary(
                user.getId(),
                user.getNickname(),
                user.getStreak(),
                level,
                totalStudied
        );
    }

    private static int calculateLevel(int totalStudied) {
        if (totalStudied >= 1000) return 10;
        if (totalStudied >= 700) return 9;
        if (totalStudied >= 500) return 8;
        if (totalStudied >= 350) return 7;
        if (totalStudied >= 250) return 6;
        if (totalStudied >= 150) return 5;
        if (totalStudied >= 100) return 4;
        if (totalStudied >= 50) return 3;
        if (totalStudied >= 20) return 2;
        return 1;
    }
}
