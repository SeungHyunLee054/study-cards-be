package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {

    Optional<Category> findByCode(String code);

    boolean existsByCode(String code);

    List<Category> findByParentIsNullOrderByDisplayOrder();

    List<Category> findByParentOrderByDisplayOrder(Category parent);

    List<Category> findByDepth(Integer depth);

    Page<Category> findByParent(Category parent, Pageable pageable);
}
