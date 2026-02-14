package com.example.study_cards.application.user.service;

import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.user.dto.request.PasswordChangeRequest;
import com.example.study_cards.application.user.dto.request.UserUpdateRequest;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.exception.UserErrorCode;
import com.example.study_cards.domain.user.exception.UserException;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.redis.service.RefreshTokenService;
import com.example.study_cards.infra.redis.service.TokenBlacklistService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import com.example.study_cards.infra.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserDomainService userDomainService;
    private final UserCacheService userCacheService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserResponse getMyInfo(Long userId) {
        User user = userDomainService.findById(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
        User user = userDomainService.findById(userId);
        user.updateNickname(request.nickname());
        userCacheService.evictUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userDomainService.findById(userId);

        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.isOAuthUser()) {
            throw new UserException(UserErrorCode.OAUTH_USER_CANNOT_CHANGE_PASSWORD);
        }

        userDomainService.validatePassword(request.currentPassword(), user.getPassword());
        userDomainService.changePassword(user, request.newPassword());
    }

    @Transactional
    public void withdrawMyAccount(Long userId, String accessToken) {
        User user = userDomainService.findById(userId);

        long remainingMs = jwtTokenProvider.getRemainingExpiration(accessToken);
        tokenBlacklistService.blacklistToken(accessToken, remainingMs);
        refreshTokenService.deleteRefreshToken(userId);
        userCacheService.evictUser(userId);

        userDomainService.withdraw(user);
    }
}
