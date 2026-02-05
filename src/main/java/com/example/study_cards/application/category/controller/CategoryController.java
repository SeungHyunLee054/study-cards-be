package com.example.study_cards.application.category.controller;

import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.application.category.dto.response.CategoryTreeResponse;
import com.example.study_cards.application.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAllCategories(
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getAllCategories(pageable));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    @GetMapping("/{code}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable String code) {
        return ResponseEntity.ok(categoryService.getCategoryByCode(code));
    }

    @GetMapping("/{code}/children")
    public ResponseEntity<Page<CategoryResponse>> getChildCategories(
            @PathVariable String code,
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoryService.getChildCategories(code, pageable));
    }
}
