package com.example.study_cards.domain.generation.repository;

import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GeneratedCardRepositoryCustom {

    List<Object[]> countByModelGroupByStatus();

    List<GeneratedCard> findByStatusWithCategory(GenerationStatus status);

    Page<GeneratedCard> findByStatusWithCategory(GenerationStatus status, Pageable pageable);
}
