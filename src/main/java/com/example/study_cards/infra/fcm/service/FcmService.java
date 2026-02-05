package com.example.study_cards.infra.fcm.service;

import com.example.study_cards.domain.notification.exception.NotificationErrorCode;
import com.example.study_cards.domain.notification.exception.NotificationException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.fcm.credentials-path")
public class FcmService {

    @Async
    public void sendNotification(String fcmToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 푸시 발송 성공 - messageId: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 푸시 발송 실패 - token: {}, error: {}", maskToken(fcmToken), e.getMessage());
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
