package com.example.study_cards.infra.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String FROM_NAME = "Study Cards";
    private static final String FROM_ADDRESS = "noreply@studycard.kr";
    private static final String FROM = FROM_NAME + " <" + FROM_ADDRESS + ">";

    @Async
    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
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

    @Async
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
        message.setTo(to);
        message.setSubject("[Study Cards] 이메일 인증 코드");
        message.setText(String.format("""
                안녕하세요.

                Study Cards 회원가입을 환영합니다!

                이메일 인증 코드: %s

                이 코드는 5분간 유효합니다.
                본인이 요청하지 않았다면 이 이메일을 무시해주세요.

                감사합니다.
                Study Cards 팀
                """, code));

        mailSender.send(message);
    }
}
