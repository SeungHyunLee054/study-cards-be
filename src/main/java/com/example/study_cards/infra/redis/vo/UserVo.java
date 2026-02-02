package com.example.study_cards.infra.redis.vo;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;

import java.io.Serializable;
import java.util.Set;

public record UserVo(
        Long id,
        String email,
        String nickname,
        Set<Role> roles
) implements Serializable {

    public static UserVo from(User user) {
        return new UserVo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRoles()
        );
    }
}
