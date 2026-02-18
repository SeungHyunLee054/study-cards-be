package com.example.study_cards.domain.category.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {

    Optional<Category> findByCode(String code);

    Optional<Category> findByCodeAndStatus(String code, CategoryStatus status);

    Optional<Category> findByNameAndStatus(String name, CategoryStatus status);

    Optional<Category> findByIdAndStatus(Long id, CategoryStatus status);

    boolean existsByCode(String code);

    List<Category> findByParentIsNullAndStatusOrderByDisplayOrder(CategoryStatus status);

    List<Category> findByParentAndStatusOrderByDisplayOrder(Category parent, CategoryStatus status);

    Page<Category> findByParentAndStatus(Category parent, CategoryStatus status, Pageable pageable);

    Page<Category> findAllByStatus(CategoryStatus status, Pageable pageable);
}
