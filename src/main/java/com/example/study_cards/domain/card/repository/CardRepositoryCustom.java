package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CardRepositoryCustom {

    List<CategoryCount> countByCategory();

    List<Card> findAllByOrderByEfFactorAsc();

    List<Card> findByCategoriesOrderByEfFactorAsc(List<Category> categories);

    Page<Card> findByCategoriesOrderByEfFactorAscWithCategory(List<Category> categories, Pageable pageable);

    Page<Card> findAllWithCategory(Pageable pageable);

    Page<Card> findByCategoriesWithCategory(List<Category> categories, Pageable pageable);

    Page<Card> findAllByOrderByEfFactorAscWithCategory(Pageable pageable);

    Page<Card> searchByKeyword(String keyword, List<Category> categories, Pageable pageable);

    record CategoryCount(Long categoryId, String categoryCode, Long count) {}
}
