package com.example.study_cards.infra.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// TODO: 메일 서비스 활성화 시 주석 해제
// 1. application-dev.yml에서 spring.mail 설정 주석 해제
// 2. 아래 @Service 주석 해제
@RequiredArgsConstructor
//@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Study Cards] 비밀번호 재설정 인증 코드");
        message.setText(String.format("""
                안녕하세요.

                비밀번호 재설정을 위한 인증 코드입니다.

                인증 코드: %s

                이 코드는 5분간 유효합니다.
                본인이 요청하지 않았다면 이 이메일을 무시해주세요.

                감사합니다.
                Study Cards 팀
                """, code));

        mailSender.send(message);
    }
}
