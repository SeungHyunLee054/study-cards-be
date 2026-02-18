package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private static final String PROVIDER_GOOGLE_GENAI = "google-genai";
    private static final String PROVIDER_OPENAI = "openai";

    private final ChatClient chatClient;

    @Value("${app.ai.provider:google-genai}")
    private String provider;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}")
    private String googleGenAiModel;

    @Value("${spring.ai.openai.chat.options.model:gpt-5-mini}")
    private String openAiModel;

    public String generateContent(String prompt) {
        String activeProvider = resolveProvider();

        try {
            String response = chatClient.prompt(prompt).call().content();
            log.info("[AI] 응답 생성 완료 - provider: {}, length: {}",
                    activeProvider,
                    response.length());
            return response;
        } catch (Exception e) {
            log.error("[AI] 응답 생성 실패 - provider: {}, error: {}",
                    activeProvider,
                    e.getMessage());
            throw new GenerationException(GenerationErrorCode.AI_GENERATION_FAILED);
        }
    }

    public String getDefaultModel() {
        return resolveModelByProvider(resolveProvider());
    }

    private String resolveProvider() {
        if (provider == null || provider.isBlank()) {
            return PROVIDER_GOOGLE_GENAI;
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveModelByProvider(String activeProvider) {
        if (PROVIDER_OPENAI.equals(activeProvider)) {
            return openAiModel;
        }
        return googleGenAiModel;
    }
}
