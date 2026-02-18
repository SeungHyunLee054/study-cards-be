package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, CardRepositoryCustom {

    Optional<Card> findByIdAndStatus(Long id, CardStatus status);

    List<Card> findByStatus(CardStatus status);

    List<Card> findByCategoryAndStatus(Category category, CardStatus status);

    List<Card> findByIdInAndCategoryAndStatus(List<Long> ids, Category category, CardStatus status);

    long countByStatus(CardStatus status);

    long countByCategoryAndStatus(Category category, CardStatus status);

    long countByCategoryInAndStatus(List<Category> categories, CardStatus status);
}
