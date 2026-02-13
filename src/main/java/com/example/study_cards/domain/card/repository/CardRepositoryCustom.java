package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CardRepositoryCustom {

    List<CategoryCount> countByCategory();

    List<Card> findAllByOrderByEfFactorAsc();

    List<Card> findByCategoryOrderByEfFactorAsc(Category category);

    List<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category);

    Page<Card> findAllWithCategory(Pageable pageable);

    Page<Card> findByCategoryWithCategory(Category category, Pageable pageable);

    Page<Card> findAllByOrderByEfFactorAscWithCategory(Pageable pageable);

    Page<Card> findByCategoryOrderByEfFactorAscWithCategory(Category category, Pageable pageable);

    Page<Card> searchByKeyword(String keyword, Category category, Pageable pageable);

    record CategoryCount(Long categoryId, String categoryCode, Long count) {}
}
