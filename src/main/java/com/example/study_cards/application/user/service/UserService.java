package com.example.study_cards.application.user.service;

import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.application.user.dto.request.PasswordChangeRequest;
import com.example.study_cards.application.user.dto.request.UserUpdateRequest;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.infra.redis.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserDomainService userDomainService;
    private final UserCacheService userCacheService;

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
        userDomainService.validatePassword(request.currentPassword(), user.getPassword());
        userDomainService.changePassword(user, request.newPassword());
    }
}
