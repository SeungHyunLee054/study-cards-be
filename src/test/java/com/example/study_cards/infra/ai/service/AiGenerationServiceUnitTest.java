package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AiGenerationServiceUnitTest extends BaseUnitTest {

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private AiGenerationService aiGenerationService;

    @Nested
    @DisplayName("generateContent")
    class GenerateContentTest {

        @Test
        @DisplayName("AI 응답을 생성한다")
        void generateContent_success() {
            // given
            String prompt = "Test prompt";
            String expectedResponse = "{\"question\": \"test\"}";
            ReflectionTestUtils.setField(aiGenerationService, "defaultModel", "gemini-2.0-flash");

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

            given(chatClient.prompt(prompt)).willReturn(requestSpec);
            given(requestSpec.options(any(ChatOptions.class))).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(expectedResponse);

            // when
            String result = aiGenerationService.generateContent(prompt);

            // then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("AI 호출 실패 시 예외를 던진다")
        void generateContent_whenFailed_throwsException() {
            // given
            String prompt = "Test prompt";
            ReflectionTestUtils.setField(aiGenerationService, "defaultModel", "gemini-2.0-flash");

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

            given(chatClient.prompt(prompt)).willReturn(requestSpec);
            given(requestSpec.options(any(ChatOptions.class))).willReturn(requestSpec);
            given(requestSpec.call()).willThrow(new RuntimeException("API error"));

            // when & then
            assertThatThrownBy(() -> aiGenerationService.generateContent(prompt))
                    .isInstanceOf(GenerationException.class);
        }
    }

    @Nested
    @DisplayName("getDefaultModel")
    class GetDefaultModelTest {

        @Test
        @DisplayName("모델명을 반환한다")
        void getDefaultModel_returnsModel() {
            // given
            ReflectionTestUtils.setField(aiGenerationService, "defaultModel", "gemini-2.0-flash");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gemini-2.0-flash");
        }
    }
}
