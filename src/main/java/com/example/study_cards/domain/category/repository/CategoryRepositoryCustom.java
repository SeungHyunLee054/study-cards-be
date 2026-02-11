package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;

import java.util.List;

public interface CategoryRepositoryCustom {

    List<Category> findRootCategoriesWithChildren();

    List<Category> findAllWithParent();
}
