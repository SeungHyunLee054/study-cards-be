package com.example.study_cards.common.util;

import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResponseUtilsTest extends BaseUnitTest {

    @Nested
    @DisplayName("extractJsonPayload")
    class ExtractJsonPayloadTest {

        @Test
        @DisplayName("일반 JSON 문자열은 그대로 반환한다")
        void extractJsonPayload_plainJson_success() {
            // given
            String response = "{\"question\":\"Q\",\"answer\":\"A\"}";

            // when
            String result = AiResponseUtils.extractJsonPayload(response);

            // then
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("Markdown code block으로 감싼 JSON도 파싱 가능한 형태로 반환한다")
        void extractJsonPayload_markdownWrapped_success() {
            // given
            String response = """
                    ```json
                    {"question":"Q","answer":"A"}
                    ```
                    """;

            // when
            String result = AiResponseUtils.extractJsonPayload(response);

            // then
            assertThat(result).isEqualTo("{\"question\":\"Q\",\"answer\":\"A\"}");
        }

        @Test
        @DisplayName("빈 응답이면 예외를 던진다")
        void extractJsonPayload_blank_throwsException() {
            // given & when & then
            assertThatThrownBy(() -> AiResponseUtils.extractJsonPayload("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
