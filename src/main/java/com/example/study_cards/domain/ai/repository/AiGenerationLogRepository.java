package com.example.study_cards.domain.ai.repository;

import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    long countByUserIdAndType(Long userId, AiGenerationType type);

    long countByUserIdAndTypeAndSuccessTrue(Long userId, AiGenerationType type);
}
