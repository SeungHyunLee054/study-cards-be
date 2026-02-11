package com.example.study_cards.infra.fcm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {

    private String credentialsPath;
}
