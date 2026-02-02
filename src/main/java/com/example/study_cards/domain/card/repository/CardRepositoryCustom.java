package com.example.study_cards.domain.card.repository;

import com.example.study_cards.domain.card.entity.Category;

import java.util.List;

public interface CardRepositoryCustom {

    List<CategoryCount> countByCategory();

    record CategoryCount(Category category, Long count) {}
}
