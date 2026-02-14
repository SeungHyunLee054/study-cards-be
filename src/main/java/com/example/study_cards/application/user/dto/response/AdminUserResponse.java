package com.example.study_cards.application.user.dto.response;

import com.example.study_cards.domain.user.entity.OAuthProvider;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        Set<Role> roles,
        OAuthProvider provider,
        UserStatus status,
        Boolean emailVerified,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                Set.copyOf(user.getRoles()),
                user.getProvider(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getCreatedAt(),
                user.getModifiedAt()
        );
    }
}
