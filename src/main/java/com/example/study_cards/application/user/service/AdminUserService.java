package com.example.study_cards.application.user.service;

import com.example.study_cards.application.user.dto.response.AdminUserResponse;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.entity.UserStatus;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserDomainService userDomainService;
    private final RefreshTokenService refreshTokenService;
    private final UserCacheService userCacheService;

    public Page<AdminUserResponse> getUsers(UserStatus status, Pageable pageable) {
        return userDomainService.findUsers(status, pageable)
                .map(AdminUserResponse::from);
    }

    public AdminUserResponse getUser(Long userId) {
        User user = userDomainService.findByIdIncludingWithdrawn(userId);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userDomainService.findByIdIncludingWithdrawn(userId);
        userDomainService.ban(user);
        refreshTokenService.deleteRefreshToken(userId);
        userCacheService.evictUser(userId);
    }
}
