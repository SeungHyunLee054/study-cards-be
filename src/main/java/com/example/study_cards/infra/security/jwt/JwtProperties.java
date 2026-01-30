package com.example.study_cards.infra.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private int accessTokenExpireMinutes;
    private int refreshTokenExpireDays;
}
