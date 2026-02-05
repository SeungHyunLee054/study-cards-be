package com.example.study_cards.application.category.service;

import com.example.study_cards.application.category.dto.request.CategoryCreateRequest;
import com.example.study_cards.application.category.dto.request.CategoryUpdateRequest;
import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.application.category.dto.response.CategoryTreeResponse;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryDomainService categoryDomainService;

    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        Page<Category> categories = categoryDomainService.findAll(pageable);
        return categories.map(CategoryResponse::from);
    }

    public List<CategoryTreeResponse> getCategoryTree() {
        return categoryDomainService.findRootCategoriesWithChildren().stream()
                .map(CategoryTreeResponse::from)
                .toList();
    }

    public CategoryResponse getCategoryByCode(String code) {
        return CategoryResponse.from(categoryDomainService.findByCode(code));
    }

    public Page<CategoryResponse> getChildCategories(String parentCode, Pageable pageable) {
        Category parent = categoryDomainService.findByCode(parentCode);
        Page<Category> categories = categoryDomainService.findByParent(parent, pageable);
        return categories.map(CategoryResponse::from);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category parent = null;
        if (request.parentCode() != null && !request.parentCode().isBlank()) {
            parent = categoryDomainService.findByCode(request.parentCode());
        }

        Category category = categoryDomainService.createCategory(
                request.code(),
                request.name(),
                parent,
                request.displayOrder()
        );

        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryDomainService.updateCategory(
                id,
                request.code(),
                request.name(),
                request.displayOrder()
        );
        return CategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryDomainService.deleteCategory(id);
    }
}
