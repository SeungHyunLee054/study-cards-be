package com.example.study_cards.domain.category.service;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.exception.CategoryErrorCode;
import com.example.study_cards.domain.category.exception.CategoryException;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryDomainService {

    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public Category findByCode(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public Category findByCodeOrNull(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return categoryRepository.findByCode(code).orElse(null);
    }

    public List<Category> findAll() {
        return categoryRepository.findAllWithParent();
    }

    public List<Category> findRootCategories() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrder();
    }

    public List<Category> findRootCategoriesWithChildren() {
        return categoryRepository.findRootCategoriesWithChildren();
    }

    public List<Category> findByParent(Category parent) {
        return categoryRepository.findByParentOrderByDisplayOrder(parent);
    }

    @Transactional
    public Category createCategory(String code, String name, Category parent, Integer displayOrder) {
        validateCodeNotExists(code);

        Category category = Category.builder()
                .code(code)
                .name(name)
                .parent(parent)
                .displayOrder(displayOrder)
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, String code, String name, Integer displayOrder) {
        Category category = findById(id);

        if (!category.getCode().equals(code)) {
            validateCodeNotExists(code);
        }

        category.update(code, name, displayOrder);
        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);

        if (category.hasChildren()) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_HAS_CHILDREN);
        }

        categoryRepository.delete(category);
    }

    public boolean existsByCode(String code) {
        return categoryRepository.existsByCode(code);
    }

    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Page<Category> findByParent(Category parent, Pageable pageable) {
        return categoryRepository.findByParent(parent, pageable);
    }

    private void validateCodeNotExists(String code) {
        if (categoryRepository.existsByCode(code)) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_CODE_ALREADY_EXISTS);
        }
    }
}
