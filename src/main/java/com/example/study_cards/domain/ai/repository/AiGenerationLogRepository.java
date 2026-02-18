package com.example.study_cards.domain.ai.repository;

import com.example.study_cards.domain.ai.entity.AiGenerationLog;
import com.example.study_cards.domain.ai.entity.AiGenerationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    long countByUserIdAndTypeAndSuccessTrue(Long userId, AiGenerationType type);

    Page<AiGenerationLog> findByUserIdAndTypeInOrderByCreatedAtDesc(Long userId, List<AiGenerationType> types, Pageable pageable);

    long deleteByTypeAndCreatedAtBefore(AiGenerationType type, LocalDateTime createdAt);

    long deleteByTypeInAndCreatedAtBefore(List<AiGenerationType> types, LocalDateTime createdAt);
}
