package com.example.study_cards.infra.security.jwt;

import com.example.study_cards.common.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CookieProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "Refresh_Token";

    private final JwtProperties jwtProperties;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int maxAge = jwtProperties.getRefreshTokenExpireDays() * 24 * 60 * 60;
        CookieUtils.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAge);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        CookieUtils.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return CookieUtils.getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
    }
}
