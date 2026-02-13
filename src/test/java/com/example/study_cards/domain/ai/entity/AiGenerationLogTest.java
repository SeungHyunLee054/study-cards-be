package com.example.study_cards.domain.ai.entity;

import com.example.study_cards.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiGenerationLogTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("success 미지정 시 기본값 true가 설정된다")
        void builder_success_null이면_기본값true() {
            // when
            AiGenerationLog log = AiGenerationLog.builder()
                    .user(testUser)
                    .type(AiGenerationType.USER_CARD)
                    .prompt("테스트 프롬프트")
                    .build();

            // then
            assertThat(log.getSuccess()).isTrue();
        }

        @Test
        @DisplayName("success를 false로 지정하면 해당 값이 설정된다")
        void builder_success_false지정() {
            // when
            AiGenerationLog log = AiGenerationLog.builder()
                    .user(testUser)
                    .type(AiGenerationType.USER_CARD)
                    .prompt("테스트 프롬프트")
                    .success(false)
                    .errorMessage("API 호출 실패")
                    .build();

            // then
            assertThat(log.getSuccess()).isFalse();
            assertThat(log.getErrorMessage()).isEqualTo("API 호출 실패");
        }
    }
}
