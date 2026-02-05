package com.example.study_cards.infra.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.toss")
public class TossPaymentProperties {

    private String clientKey;
    private String secretKey;
    private String webhookSecret;
    private String apiUrl = "https://api.tosspayments.com/v1";
}
