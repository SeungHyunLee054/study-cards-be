package com.example.study_cards.application.auth.dto.response;

public record TokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
