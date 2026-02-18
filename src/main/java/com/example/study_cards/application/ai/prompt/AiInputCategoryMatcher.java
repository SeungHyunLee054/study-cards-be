package com.example.study_cards.application.ai.prompt;

import com.example.study_cards.common.util.AiCategoryType;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AiInputCategoryMatcher {

    private static final Pattern JAPANESE_PATTERN = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern LATIN_PATTERN = Pattern.compile("[A-Za-z]");

    private static final List<String> JLPT_HINTS = List.of(
            "jlpt", "일본어", "히라가나", "가타카나", "한자", "문형", "문법"
    );
    private static final List<String> TOEIC_HINTS = List.of(
            "toeic", "토익", "영어", "part 5", "part5", "rc", "lc"
    );

    private AiInputCategoryMatcher() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static boolean isLikelyMatch(AiCategoryType categoryType, String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return true;
        }

        String normalized = sourceText.toLowerCase(Locale.ROOT);

        return switch (categoryType) {
            case JLPT -> hasEnoughJapaneseCharacters(sourceText) || containsAny(normalized, JLPT_HINTS);
            case TOEIC -> hasEnoughLatinCharacters(sourceText) || containsAny(normalized, TOEIC_HINTS);
            case CS, GENERIC -> true;
        };
    }

    private static boolean hasEnoughJapaneseCharacters(String text) {
        return countMatches(JAPANESE_PATTERN, text) >= 5;
    }

    private static boolean hasEnoughLatinCharacters(String text) {
        return countMatches(LATIN_PATTERN, text) >= 15;
    }

    private static int countMatches(Pattern pattern, String text) {
        int count = 0;
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static boolean containsAny(String text, List<String> hints) {
        for (String hint : hints) {
            if (text.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
