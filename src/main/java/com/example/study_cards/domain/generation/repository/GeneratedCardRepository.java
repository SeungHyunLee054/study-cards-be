package com.example.study_cards.domain.generation.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedCardRepository extends JpaRepository<GeneratedCard, Long>, GeneratedCardRepositoryCustom {

    Page<GeneratedCard> findByStatus(GenerationStatus status, Pageable pageable);

    Page<GeneratedCard> findByStatusAndModel(GenerationStatus status, String model, Pageable pageable);

    Page<GeneratedCard> findByStatusAndCategory(GenerationStatus status, Category category, Pageable pageable);

    Page<GeneratedCard> findByModel(String model, Pageable pageable);

    long countByStatus(GenerationStatus status);

    long countByModel(String model);

    long countByModelAndStatus(String model, GenerationStatus status);
}
