package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, CardRepositoryCustom {

    Optional<Card> findByIdAndStatus(Long id, CardStatus status);

    List<Card> findByStatus(CardStatus status);

    List<Card> findByCategoryAndStatus(Category category, CardStatus status);

    long countByStatus(CardStatus status);

    long countByCategoryAndStatus(Category category, CardStatus status);

    @Query("""
            select count(c) > 0
            from Card c
            where c.category = :category
              and c.status = :status
            """)
    boolean existsByCategoryAndStatus(@Param("category") Category category, @Param("status") CardStatus status);
}
