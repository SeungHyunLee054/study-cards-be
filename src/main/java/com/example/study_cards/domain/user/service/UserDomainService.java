package com.example.study_cards.domain.user.service;

import com.example.study_cards.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDomainService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // TODO: 회원가입 로직

    // TODO: 이메일로 사용자 조회

    // TODO: 비밀번호 검증

    // TODO: 사용자 통계 업데이트 (streak, masteryRate)
}
