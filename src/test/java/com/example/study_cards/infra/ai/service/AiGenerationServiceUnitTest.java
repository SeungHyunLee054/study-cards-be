package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.infra.ai.config.AiProviderProperties;
import com.example.study_cards.infra.ai.config.GoogleGenAiChatOptionsProperties;
import com.example.study_cards.infra.ai.config.OpenAiChatOptionsProperties;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AiGenerationServiceUnitTest extends BaseUnitTest {

    @Mock
    private ChatClient chatClient;
    private AiGenerationService aiGenerationService;
    private AiProviderProperties aiProviderProperties;
    private GoogleGenAiChatOptionsProperties googleGenAiChatOptionsProperties;
    private OpenAiChatOptionsProperties openAiChatOptionsProperties;

    @BeforeEach
    void setUp() {
        aiProviderProperties = new AiProviderProperties();
        googleGenAiChatOptionsProperties = new GoogleGenAiChatOptionsProperties();
        openAiChatOptionsProperties = new OpenAiChatOptionsProperties();
        aiGenerationService = new AiGenerationService(
                chatClient,
                aiProviderProperties,
                googleGenAiChatOptionsProperties,
                openAiChatOptionsProperties
        );
    }

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

        @Test
        @DisplayName("AI 응답이 비어있으면 예외를 던진다")
        void generateContent_whenEmptyResponse_throwsException() {
            // given
            String prompt = "Test prompt";

            ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

            given(chatClient.prompt(prompt)).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn("   ");

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
            aiProviderProperties.setProvider("openai");
            googleGenAiChatOptionsProperties.setModel("gemini-2.0-flash");
            openAiChatOptionsProperties.setModel("gpt-5-mini");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gpt-5-mini");
        }

        @Test
        @DisplayName("Provider가 google-genai면 gemini 모델을 반환한다")
        void getDefaultModel_google_returnsGeminiModel() {
            // given
            aiProviderProperties.setProvider("google-genai");
            googleGenAiChatOptionsProperties.setModel("gemini-2.0-flash");
            openAiChatOptionsProperties.setModel("gpt-5-mini");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gemini-2.0-flash");
        }

        @Test
        @DisplayName("Provider 미설정 시 google-genai 모델을 기본으로 사용한다")
        void getDefaultModel_blankProvider_returnsGeminiModel() {
            // given
            aiProviderProperties.setProvider("   ");
            googleGenAiChatOptionsProperties.setModel("gemini-2.0-flash");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("gemini-2.0-flash");
        }

        @Test
        @DisplayName("모델이 비어있으면 unknown을 반환한다")
        void getDefaultModel_blankModel_returnsUnknown() {
            // given
            aiProviderProperties.setProvider("openai");
            openAiChatOptionsProperties.setModel(" ");

            // when
            String result = aiGenerationService.getDefaultModel();

            // then
            assertThat(result).isEqualTo("unknown");
        }
    }
}
