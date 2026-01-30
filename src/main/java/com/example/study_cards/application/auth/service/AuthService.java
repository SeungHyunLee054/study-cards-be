package com.example.study_cards.application.auth.service;

import com.example.study_cards.application.auth.dto.request.SignInRequest;
import com.example.study_cards.application.auth.dto.request.SignUpRequest;
import com.example.study_cards.application.auth.dto.response.SignInResponse;
import com.example.study_cards.application.auth.dto.response.UserResponse;
import com.example.study_cards.domain.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserDomainService userDomainService;

    @Transactional
    public UserResponse signUp(SignUpRequest request) {
        // TODO: userDomainService 호출
        return null;
    }

    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        // TODO: userDomainService 호출, JWT 토큰 생성
        return null;
    }

    @Transactional
    public void signOut(String accessToken) {
        // TODO: Redis 블랙리스트 처리
    }
}
