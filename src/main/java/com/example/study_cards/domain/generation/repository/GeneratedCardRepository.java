package com.example.study_cards.domain.generation.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.generation.entity.GeneratedCard;
import com.example.study_cards.domain.generation.entity.GenerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeneratedCardRepository extends JpaRepository<GeneratedCard, Long> {

    Page<GeneratedCard> findByStatus(GenerationStatus status, Pageable pageable);

    Page<GeneratedCard> findByStatusAndModel(GenerationStatus status, String model, Pageable pageable);

    Page<GeneratedCard> findByStatusAndCategory(GenerationStatus status, Category category, Pageable pageable);

    Page<GeneratedCard> findByModel(String model, Pageable pageable);

    List<GeneratedCard> findByStatus(GenerationStatus status);

    long countByStatus(GenerationStatus status);

    long countByModel(String model);

    long countByModelAndStatus(String model, GenerationStatus status);

    @Query("SELECT gc.model, gc.status, COUNT(gc) FROM GeneratedCard gc GROUP BY gc.model, gc.status")
    List<Object[]> countByModelGroupByStatus();

    @Query("SELECT gc FROM GeneratedCard gc JOIN FETCH gc.category WHERE gc.status = :status")
    List<GeneratedCard> findByStatusWithCategory(@Param("status") GenerationStatus status);

    @Query(value = "SELECT gc FROM GeneratedCard gc JOIN FETCH gc.category WHERE gc.status = :status",
           countQuery = "SELECT COUNT(gc) FROM GeneratedCard gc WHERE gc.status = :status")
    Page<GeneratedCard> findByStatusWithCategory(@Param("status") GenerationStatus status, Pageable pageable);
}
