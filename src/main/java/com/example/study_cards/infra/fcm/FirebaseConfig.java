package com.example.study_cards.infra.fcm;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fcm.credentials-path")
public class FirebaseConfig {

    private final FcmProperties fcmProperties;

    @PostConstruct
    public void initialize() {
        try {
            // ClassPathResource resource = new ClassPathResource(fcmProperties.getCredentialsPath());
			FileSystemResource resource = new FileSystemResource(fcmProperties.getCredentialsPath());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK 초기화 완료");
            }
        } catch (IOException e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
            throw new RuntimeException("Firebase Admin SDK 초기화에 실패했습니다.", e);
        }
    }
}
