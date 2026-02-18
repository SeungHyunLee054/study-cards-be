package com.example.study_cards.domain.ai.service;

import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AiGenerationLogDomainService {

    private final AiGenerationLogRepository aiGenerationLogRepository;

    public AiGenerationLog save(AiGenerationLog log) {
        return aiGenerationLogRepository.save(log);
    }

    public long countByUserIdAndTypeAndSuccessTrue(Long userId, AiGenerationType type) {
        return aiGenerationLogRepository.countByUserIdAndTypeAndSuccessTrue(userId, type);
    }

    public Page<AiGenerationLog> findByUserIdAndTypeInOrderByCreatedAtDesc(
            Long userId,
            List<AiGenerationType> types,
            Pageable pageable
    ) {
        return aiGenerationLogRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, types, pageable);
    }

    public long deleteByTypeAndCreatedAtBefore(AiGenerationType type, LocalDateTime createdAt) {
        return aiGenerationLogRepository.deleteByTypeAndCreatedAtBefore(type, createdAt);
    }

    public long deleteByTypeInAndCreatedAtBefore(List<AiGenerationType> types, LocalDateTime createdAt) {
        return aiGenerationLogRepository.deleteByTypeInAndCreatedAtBefore(types, createdAt);
    }
}
