package com.example.study_cards.application.ai.prompt;

import com.example.study_cards.common.util.AiCategoryType;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiInputCategoryMatcherTest extends BaseUnitTest {

    @Nested
    @DisplayName("isLikelyMatch")
    class IsLikelyMatchTest {

        @Test
        @DisplayName("입력 텍스트가 null이면 true를 반환한다")
        void isLikelyMatch_nullText_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(AiCategoryType.JLPT, null);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("입력 텍스트가 공백이면 true를 반환한다")
        void isLikelyMatch_blankText_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(AiCategoryType.TOEIC, "   ");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("JLPT는 일본어 문자수가 충분하면 true를 반환한다")
        void isLikelyMatch_jlptWithJapaneseChars_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.JLPT,
                    "日本語の文法を勉強します"
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("JLPT는 일본어 힌트가 있으면 true를 반환한다")
        void isLikelyMatch_jlptWithHint_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.JLPT,
                    "일본어 문법 포인트를 정리해줘"
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("JLPT 조건이 없으면 false를 반환한다")
        void isLikelyMatch_jlptWithoutSignal_returnsFalse() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.JLPT,
                    "운영체제 프로세스 상태와 스케줄링"
            );

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("TOEIC은 영문 문자수가 충분하면 true를 반환한다")
        void isLikelyMatch_toeicWithLatinChars_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.TOEIC,
                    "This sentence has enough alphabet characters to pass threshold."
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TOEIC은 힌트가 있으면 true를 반환한다")
        void isLikelyMatch_toeicWithHint_returnsTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.TOEIC,
                    "toeic part5 문제 유형 정리"
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TOEIC 조건이 없으면 false를 반환한다")
        void isLikelyMatch_toeicWithoutSignal_returnsFalse() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.TOEIC,
                    "자료구조와 알고리즘 핵심 정리"
            );

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("CS 카테고리는 항상 true를 반환한다")
        void isLikelyMatch_csAlwaysTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.CS,
                    "어떤 텍스트든 통과"
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GENERIC 카테고리는 항상 true를 반환한다")
        void isLikelyMatch_genericAlwaysTrue() {
            boolean result = AiInputCategoryMatcher.isLikelyMatch(
                    AiCategoryType.GENERIC,
                    "무관한 텍스트"
            );

            assertThat(result).isTrue();
        }
    }
}
