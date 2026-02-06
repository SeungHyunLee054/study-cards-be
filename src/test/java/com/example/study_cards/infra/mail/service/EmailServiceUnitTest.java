package com.example.study_cards.infra.mail.service;

import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class EmailServiceUnitTest extends BaseUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("sendPasswordResetCode")
    class SendPasswordResetCodeTest {

        @Test
        @DisplayName("비밀번호 재설정 코드 이메일을 발송한다")
        void sendPasswordResetCode_success() {
            // given
            String to = "test@example.com";
            String code = "123456";

            // when
            emailService.sendPasswordResetCode(to, code);

            // then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).contains(to);
            assertThat(sentMessage.getSubject()).contains("비밀번호 재설정");
            assertThat(sentMessage.getText()).contains(code);
            assertThat(sentMessage.getText()).contains("5분간 유효");
        }

        @Test
        @DisplayName("발신자 정보가 올바르게 설정된다")
        void sendPasswordResetCode_fromAddressCorrect() {
            // given
            String to = "test@example.com";
            String code = "123456";

            // when
            emailService.sendPasswordResetCode(to, code);

            // then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getFrom()).contains("Study Cards");
            assertThat(sentMessage.getFrom()).contains("noreply@studycard.kr");
        }
    }

    @Nested
    @DisplayName("sendVerificationCode")
    class SendVerificationCodeTest {

        @Test
        @DisplayName("이메일 인증 코드를 발송한다")
        void sendVerificationCode_success() {
            // given
            String to = "test@example.com";
            String code = "654321";

            // when
            emailService.sendVerificationCode(to, code);

            // then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).contains(to);
            assertThat(sentMessage.getSubject()).contains("이메일 인증 코드");
            assertThat(sentMessage.getText()).contains(code);
            assertThat(sentMessage.getText()).contains("회원가입을 환영합니다");
        }

        @Test
        @DisplayName("이메일 본문에 인증 코드가 포함된다")
        void sendVerificationCode_containsCode() {
            // given
            String to = "user@example.com";
            String code = "999888";

            // when
            emailService.sendVerificationCode(to, code);

            // then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getText()).contains("이메일 인증 코드: " + code);
        }

        @Test
        @DisplayName("이메일 유효 시간 안내가 포함된다")
        void sendVerificationCode_containsValidityInfo() {
            // given
            String to = "user@example.com";
            String code = "111222";

            // when
            emailService.sendVerificationCode(to, code);

            // then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getText()).contains("5분간 유효");
        }
    }
}
