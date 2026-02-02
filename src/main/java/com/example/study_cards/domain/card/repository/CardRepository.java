package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>, CardRepositoryCustom {

    List<Card> findByCategory(Category category);

    List<Card> findAllByOrderByEfFactorAsc();

    List<Card> findByCategoryOrderByEfFactorAsc(Category category);

    long countByCategory(Category category);
}
