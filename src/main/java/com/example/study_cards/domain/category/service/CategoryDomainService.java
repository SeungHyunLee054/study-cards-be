package com.example.study_cards.domain.category.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.category.exception.CategoryErrorCode;
import com.example.study_cards.domain.category.exception.CategoryException;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryDomainService {

    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;

    public Category findById(Long id) {
        return categoryRepository.findByIdAndStatus(id, CategoryStatus.ACTIVE)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public Category findByCode(String code) {
        return categoryRepository.findByCodeAndStatus(code, CategoryStatus.ACTIVE)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public Category findByCodeOrNull(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return categoryRepository.findByCodeAndStatus(code, CategoryStatus.ACTIVE).orElse(null);
    }

    public Category findByNameOrNull(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return categoryRepository.findByNameAndStatus(name, CategoryStatus.ACTIVE).orElse(null);
    }

    public List<Category> findAll() {
        return categoryRepository.findAllWithParent();
    }

    public List<Category> findLeafCategories() {
        List<Category> allCategories = categoryRepository.findAllWithParent();
        Set<Long> parentIds = allCategories.stream()
                .map(Category::getParent)
                .filter(parent -> parent != null)
                .map(Category::getId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        return allCategories.stream()
                .filter(category -> !parentIds.contains(category.getId()))
                .toList();
    }

    public List<Category> findRootCategoriesWithChildren() {
        return categoryRepository.findRootCategoriesWithChildren();
    }

    public List<Category> findByParent(Category parent) {
        return categoryRepository.findByParentAndStatusOrderByDisplayOrder(parent, CategoryStatus.ACTIVE);
    }

    public boolean isLeafCategory(Category category) {
        if (category == null) {
            return false;
        }
        return categoryRepository.findByParentAndStatusOrderByDisplayOrder(category, CategoryStatus.ACTIVE).isEmpty();
    }

    public void validateLeafCategory(Category category) {
        if (!isLeafCategory(category)) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_NOT_LEAF);
        }
    }

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

    public Category updateCategory(Long id, String code, String name, Integer displayOrder) {
        Category category = findById(id);

        if (!category.getCode().equals(code)) {
            validateCodeNotExists(code);
        }

        category.update(code, name, displayOrder);
        return category;
    }

    public void deleteCategory(Long id) {
        Category category = findById(id);

        if (category.hasChildren()) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_HAS_CHILDREN);
        }

        List<Card> cardsInCategory = cardRepository.findByCategoryAndStatus(category, CardStatus.ACTIVE);
        cardsInCategory.forEach(Card::delete);
        category.delete();
    }

    public boolean existsByCode(String code) {
        return categoryRepository.existsByCode(code);
    }

    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAllByStatus(CategoryStatus.ACTIVE, pageable);
    }

    public Page<Category> findByParent(Category parent, Pageable pageable) {
        return categoryRepository.findByParentAndStatus(parent, CategoryStatus.ACTIVE, pageable);
    }

    public List<Category> findSelfAndDescendants(Category root) {
        List<Category> allCategories = categoryRepository.findAllWithParent();
        Map<Long, List<Category>> childrenByParent = new HashMap<>();

        for (Category category : allCategories) {
            Category parent = category.getParent();
            if (parent != null) {
                childrenByParent.computeIfAbsent(parent.getId(), key -> new ArrayList<>()).add(category);
            }
        }

        List<Category> result = new ArrayList<>();
        ArrayDeque<Category> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Category current = queue.poll();
            result.add(current);

            List<Category> children = childrenByParent.get(current.getId());
            if (children != null) {
                queue.addAll(children);
            }
        }

        return result;
    }

    private void validateCodeNotExists(String code) {
        if (categoryRepository.existsByCode(code)) {
            throw new CategoryException(CategoryErrorCode.CATEGORY_CODE_ALREADY_EXISTS);
        }
    }
}
