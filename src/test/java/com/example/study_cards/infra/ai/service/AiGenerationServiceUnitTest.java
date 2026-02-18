package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

            given(chatClient.prompt(prompt)).willReturn(requestSpec);
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

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

            given(chatClient.prompt(prompt)).willReturn(requestSpec);
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
        @DisplayName("Provider가 openai면 openai 모델을 반환한다")
        void getDefaultModel_openAi_returnsOpenAiModel() {
            // given
            ReflectionTestUtils.setField(aiGenerationService, "provider", "openai");
            ReflectionTestUtils.setField(aiGenerationService, "openAiModel", "gpt-5-mini");
            ReflectionTestUtils.setField(aiGenerationService, "googleGenAiModel", "gemini-2.0-flash");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gpt-5-mini");
        }

        @Test
        @DisplayName("Provider가 google-genai면 gemini 모델을 반환한다")
        void getDefaultModel_google_returnsGeminiModel() {
            // given
            ReflectionTestUtils.setField(aiGenerationService, "provider", "google-genai");
            ReflectionTestUtils.setField(aiGenerationService, "openAiModel", "gpt-5-mini");
            ReflectionTestUtils.setField(aiGenerationService, "googleGenAiModel", "gemini-2.0-flash");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gemini-2.0-flash");
        }
    }
}
