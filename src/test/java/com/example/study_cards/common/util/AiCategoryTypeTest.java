package com.example.study_cards.common.util;

import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiCategoryTypeTest extends BaseUnitTest {

    @Nested
    @DisplayName("fromCode")
    class FromCodeTest {

        @Test
        @DisplayName("카테고리 코드를 AI 카테고리 타입으로 분류한다")
        void fromCode_classifiesCategoryType() {
            // given & when & then
            assertThat(AiCategoryType.fromCode("CS")).isEqualTo(AiCategoryType.CS);
            assertThat(AiCategoryType.fromCode("CS_OS")).isEqualTo(AiCategoryType.CS);
            assertThat(AiCategoryType.fromCode("TOEIC")).isEqualTo(AiCategoryType.TOEIC);
            assertThat(AiCategoryType.fromCode("EN_VOCAB")).isEqualTo(AiCategoryType.TOEIC);
            assertThat(AiCategoryType.fromCode("JLPT_N3")).isEqualTo(AiCategoryType.JLPT);
            assertThat(AiCategoryType.fromCode("JN_N2")).isEqualTo(AiCategoryType.JLPT);
            assertThat(AiCategoryType.fromCode("HISTORY")).isEqualTo(AiCategoryType.GENERIC);
        }
    }

    @Nested
    @DisplayName("toJlptLevel")
    class ToJlptLevelTest {

        @Test
        @DisplayName("JLPT 코드에서 레벨 문자열을 추출한다")
        void toJlptLevel_extractsLevel() {
            // given & when & then
            assertThat(AiCategoryType.toJlptLevel("JN_N2")).isEqualTo("N2");
            assertThat(AiCategoryType.toJlptLevel("JLPT_N4")).isEqualTo("N4");
        }
    }
}
