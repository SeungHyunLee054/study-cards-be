package com.example.study_cards.application.auth.dto.response;

public record SignInResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public SignInResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}
