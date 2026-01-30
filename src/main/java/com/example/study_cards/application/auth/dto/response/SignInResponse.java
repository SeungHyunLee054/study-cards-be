package com.example.study_cards.application.auth.dto.response;

public record SignInResponse(
        String accessToken,
        String refreshToken
) {
}
