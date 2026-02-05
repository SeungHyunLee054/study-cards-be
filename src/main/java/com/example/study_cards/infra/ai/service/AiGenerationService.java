package com.example.study_cards.infra.ai.service;

import com.example.study_cards.domain.generation.exception.GenerationErrorCode;
import com.example.study_cards.domain.generation.exception.GenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private final ChatClient chatClient;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}")
    private String defaultModel;

    public String generateContent(String prompt) {
        return generateContent(prompt, null);
    }

    public String generateContent(String prompt, String model) {
        String targetModel = (model == null || model.isBlank()) ? defaultModel : model;

        try {
            String response = chatClient.prompt(prompt)
                    .options(GoogleGenAiChatOptions.builder()
                            .model(targetModel)
                            .build())
                    .call()
                    .content();
            log.info("[AI] 응답 생성 완료 - model: {}, length: {}", targetModel, response.length());
            return response;
        } catch (Exception e) {
            log.error("[AI] 응답 생성 실패 - model: {}, error: {}", targetModel, e.getMessage());
            throw new GenerationException(GenerationErrorCode.AI_GENERATION_FAILED);
        }
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
