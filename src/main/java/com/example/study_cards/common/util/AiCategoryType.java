package com.example.study_cards.common.util;

import java.util.Locale;

public enum AiCategoryType {
    CS,
    TOEIC,
    JLPT,
    GENERIC;

    public static AiCategoryType fromCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return GENERIC;
        }

        String normalized = categoryCode.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("JLPT") || normalized.startsWith("JN_")) {
            return JLPT;
        }
        if (normalized.equals("TOEIC") || normalized.startsWith("EN_")) {
            return TOEIC;
        }
        if (normalized.equals("CS") || normalized.startsWith("CS_")) {
            return CS;
        }
        return GENERIC;
    }

    public boolean isQuizType() {
        return this == TOEIC || this == JLPT;
    }

    public static String toJlptLevel(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return "";
        }

        String normalized = categoryCode.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("JN_")) {
            return normalized.substring(3);
        }
        if (normalized.startsWith("JLPT_")) {
            return normalized.substring(5);
        }
        return normalized;
    }
}
