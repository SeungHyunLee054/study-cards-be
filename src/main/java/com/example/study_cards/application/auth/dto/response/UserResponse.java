package com.example.study_cards.application.auth.dto.response;

import com.example.study_cards.domain.user.entity.OAuthProvider;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Set<Role> roles,
        OAuthProvider provider,
        Integer streak
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRoles(),
                user.getProvider(),
                user.getStreak()
        );
    }
}
