package com.example.study_cards.application.category.service;

import com.example.study_cards.application.category.dto.request.CategoryCreateRequest;
import com.example.study_cards.application.category.dto.request.CategoryUpdateRequest;
import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.application.category.dto.response.CategoryTreeResponse;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class CategoryServiceUnitTest extends BaseUnitTest {

    @Mock
    private CategoryDomainService categoryDomainService;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = Category.builder()
                .code("CS")
                .name("Computer Science")
                .build();
        setId(rootCategory, 1L);

        childCategory = Category.builder()
                .code("ALGO")
                .name("Algorithm")
                .parent(rootCategory)
                .build();
        setId(childCategory, 2L);
    }

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("모든 카테고리를 페이지로 조회한다")
        void getAllCategories_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> categoryPage = new PageImpl<>(List.of(rootCategory, childCategory), pageable, 2);
            given(categoryDomainService.findAll(pageable)).willReturn(categoryPage);

            // when
            Page<CategoryResponse> result = categoryService.getAllCategories(pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).code()).isEqualTo("CS");
            assertThat(result.getContent().get(1).code()).isEqualTo("ALGO");
        }
    }

    @Nested
    @DisplayName("getCategoryTree")
    class GetCategoryTreeTest {

        @Test
        @DisplayName("카테고리 트리를 조회한다")
        void getCategoryTree_success() {
            // given
            given(categoryDomainService.findRootCategoriesWithChildren())
                    .willReturn(List.of(rootCategory));

            // when
            List<CategoryTreeResponse> result = categoryService.getCategoryTree();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("CS");
        }
    }

    @Nested
    @DisplayName("getCategoryByCode")
    class GetCategoryByCodeTest {

        @Test
        @DisplayName("코드로 카테고리를 조회한다")
        void getCategoryByCode_success() {
            // given
            given(categoryDomainService.findByCode("CS")).willReturn(rootCategory);

            // when
            CategoryResponse result = categoryService.getCategoryByCode("CS");

            // then
            assertThat(result.code()).isEqualTo("CS");
            assertThat(result.name()).isEqualTo("Computer Science");
        }
    }

    @Nested
    @DisplayName("getChildCategories")
    class GetChildCategoriesTest {

        @Test
        @DisplayName("자식 카테고리를 조회한다")
        void getChildCategories_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> categoryPage = new PageImpl<>(List.of(childCategory), pageable, 1);

            given(categoryDomainService.findByCode("CS")).willReturn(rootCategory);
            given(categoryDomainService.findByParent(rootCategory, pageable)).willReturn(categoryPage);

            // when
            Page<CategoryResponse> result = categoryService.getChildCategories("CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).code()).isEqualTo("ALGO");
            assertThat(result.getContent().get(0).parentCode()).isEqualTo("CS");
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTest {

        @Test
        @DisplayName("루트 카테고리를 생성한다")
        void createCategory_root_success() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest("NEW", "New Category", null, 1);
            Category newCategory = Category.builder()
                    .code("NEW")
                    .name("New Category")
                    .build();
            setId(newCategory, 3L);

            given(categoryDomainService.createCategory("NEW", "New Category", null, 1))
                    .willReturn(newCategory);

            // when
            CategoryResponse result = categoryService.createCategory(request);

            // then
            assertThat(result.code()).isEqualTo("NEW");
            assertThat(result.name()).isEqualTo("New Category");
            assertThat(result.parentCode()).isNull();
        }

        @Test
        @DisplayName("자식 카테고리를 생성한다")
        void createCategory_withParent_success() {
            // given
            CategoryCreateRequest request = new CategoryCreateRequest("CHILD", "Child Category", "CS", 1);
            Category newCategory = Category.builder()
                    .code("CHILD")
                    .name("Child Category")
                    .parent(rootCategory)
                    .build();
            setId(newCategory, 3L);

            given(categoryDomainService.findByCode("CS")).willReturn(rootCategory);
            given(categoryDomainService.createCategory("CHILD", "Child Category", rootCategory, 1))
                    .willReturn(newCategory);

            // when
            CategoryResponse result = categoryService.createCategory(request);

            // then
            assertThat(result.code()).isEqualTo("CHILD");
            assertThat(result.parentCode()).isEqualTo("CS");
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTest {

        @Test
        @DisplayName("카테고리를 수정한다")
        void updateCategory_success() {
            // given
            CategoryUpdateRequest request = new CategoryUpdateRequest("UPDATED", "Updated Name", 2);
            Category updatedCategory = Category.builder()
                    .code("UPDATED")
                    .name("Updated Name")
                    .build();
            setId(updatedCategory, 1L);

            given(categoryDomainService.updateCategory(1L, "UPDATED", "Updated Name", 2))
                    .willReturn(updatedCategory);

            // when
            CategoryResponse result = categoryService.updateCategory(1L, request);

            // then
            assertThat(result.code()).isEqualTo("UPDATED");
            assertThat(result.name()).isEqualTo("Updated Name");
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTest {

        @Test
        @DisplayName("카테고리를 삭제한다")
        void deleteCategory_success() {
            // when
            categoryService.deleteCategory(1L);

            // then
            verify(categoryDomainService).deleteCategory(1L);
        }
    }

    private void setId(Category category, Long id) {
        try {
            java.lang.reflect.Field field = Category.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(category, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
