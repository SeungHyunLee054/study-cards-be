package com.example.study_cards.common.util;

public final class AiResponseUtils {

    private AiResponseUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static String extractJsonPayload(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("AI response is empty");
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        String cleaned = trimmed.trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("AI response payload is empty");
        }

        return cleaned;
    }
}
