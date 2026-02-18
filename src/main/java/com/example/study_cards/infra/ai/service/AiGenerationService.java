package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;
import com.example.study_cards.infra.ai.config.AiProviderProperties;
import com.example.study_cards.infra.ai.config.GoogleGenAiChatOptionsProperties;
import com.example.study_cards.infra.ai.config.OpenAiChatOptionsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private static final String PROVIDER_GOOGLE_GENAI = "google-genai";
    private static final String PROVIDER_OPENAI = "openai";

    private final ChatClient chatClient;
    private final AiProviderProperties aiProviderProperties;
    private final GoogleGenAiChatOptionsProperties googleGenAiChatOptionsProperties;
    private final OpenAiChatOptionsProperties openAiChatOptionsProperties;

    public String generateContent(String prompt) {
        String activeProvider = resolveProvider();

        try {
            String response = chatClient.prompt(prompt).call().content();
            if (response == null || response.isBlank()) {
                log.error("[AI] 응답 생성 실패 - provider: {}, error: empty response", activeProvider);
                throw new GenerationException(GenerationErrorCode.AI_GENERATION_FAILED);
            }

            log.info("[AI] 응답 생성 완료 - provider: {}, length: {}",
                    activeProvider,
                    response.length());
            return response;
        } catch (GenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI] 응답 생성 실패 - provider: {}, error: {}",
                    activeProvider,
                    e.getMessage());
            throw new GenerationException(GenerationErrorCode.AI_GENERATION_FAILED);
        }
    }

    public String getDefaultModel() {
        String model = resolveModelByProvider(resolveProvider());
        return (model == null || model.isBlank()) ? "unknown" : model;
    }

    private String resolveProvider() {
        String provider = aiProviderProperties.getProvider();
        if (provider == null || provider.isBlank()) {
            return PROVIDER_GOOGLE_GENAI;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveModelByProvider(String activeProvider) {
        if (PROVIDER_OPENAI.equals(activeProvider)) {
            return openAiChatOptionsProperties.getModel();
        }
        return googleGenAiChatOptionsProperties.getModel();
    }
}
