package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryCustom {

    Optional<Category> findByCodeWithParent(String code);

    List<Category> findRootCategoriesWithChildren();

    List<Category> findAllWithParent();
}
