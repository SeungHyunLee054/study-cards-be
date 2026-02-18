package com.example.study_cards.domain.category.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.entity.CategoryStatus;
import com.example.study_cards.domain.category.exception.CategoryErrorCode;
import com.example.study_cards.domain.category.exception.CategoryException;
import com.example.study_cards.domain.category.repository.CategoryRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class CategoryDomainServiceTest extends BaseUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CategoryDomainService categoryDomainService;

    private Category rootCategory;
    private Category childCategory;

    private static final Long ROOT_CATEGORY_ID = 1L;
    private static final Long CHILD_CATEGORY_ID = 2L;

    @BeforeEach
    void setUp() {
        rootCategory = createCategory(ROOT_CATEGORY_ID, "CS", "컴퓨터 과학", null, 1);
        childCategory = createCategory(CHILD_CATEGORY_ID, "CS_ALGO", "알고리즘", rootCategory, 1);
    }

    private Category createCategory(Long id, String code, String name, Category parent, int displayOrder) {
        Category category = Category.builder()
                .code(code)
                .name(name)
                .parent(parent)
                .displayOrder(displayOrder)
                .build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("존재하는 ID로 카테고리를 조회한다")
        void findById_returnsCategory() {
            // given
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));

            // when
            Category result = categoryDomainService.findById(ROOT_CATEGORY_ID);

            // then
            assertThat(result.getId()).isEqualTo(ROOT_CATEGORY_ID);
            assertThat(result.getCode()).isEqualTo("CS");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
        void findById_withNonExistentId_throwsException() {
            // given
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryDomainService.findById(ROOT_CATEGORY_ID))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findByCode")
    class FindByCodeTest {

        @Test
        @DisplayName("존재하는 코드로 카테고리를 조회한다")
        void findByCode_returnsCategory() {
            // given
            given(categoryRepository.findByCodeAndStatus("CS", CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));

            // when
            Category result = categoryDomainService.findByCode("CS");

            // then
            assertThat(result.getCode()).isEqualTo("CS");
            assertThat(result.getName()).isEqualTo("컴퓨터 과학");
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회 시 예외를 발생시킨다")
        void findByCode_withNonExistentCode_throwsException() {
            // given
            given(categoryRepository.findByCodeAndStatus("INVALID", CategoryStatus.ACTIVE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryDomainService.findByCode("INVALID"))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findByCodeOrNull")
    class FindByCodeOrNullTest {

        @Test
        @DisplayName("존재하는 코드로 카테고리를 조회한다")
        void findByCodeOrNull_returnsCategory() {
            // given
            given(categoryRepository.findByCodeAndStatus("CS", CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));

            // when
            Category result = categoryDomainService.findByCodeOrNull("CS");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("CS");
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회 시 null을 반환한다")
        void findByCodeOrNull_withNonExistentCode_returnsNull() {
            // given
            given(categoryRepository.findByCodeAndStatus("INVALID", CategoryStatus.ACTIVE)).willReturn(Optional.empty());

            // when
            Category result = categoryDomainService.findByCodeOrNull("INVALID");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 코드로 조회 시 null을 반환한다")
        void findByCodeOrNull_withNullCode_returnsNull() {
            // when
            Category result = categoryDomainService.findByCodeOrNull(null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 코드로 조회 시 null을 반환한다")
        void findByCodeOrNull_withBlankCode_returnsNull() {
            // when
            Category result = categoryDomainService.findByCodeOrNull("   ");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("모든 카테고리를 조회한다")
        void findAll_returnsAllCategories() {
            // given
            given(categoryRepository.findAllWithParent()).willReturn(List.of(rootCategory, childCategory));

            // when
            List<Category> result = categoryDomainService.findAll();

            // then
            assertThat(result).hasSize(2);
            verify(categoryRepository).findAllWithParent();
        }
    }

    @Nested
    @DisplayName("findByParent")
    class FindByParentTest {

        @Test
        @DisplayName("부모 카테고리로 하위 카테고리를 조회한다")
        void findByParent_returnsChildCategories() {
            // given
            given(categoryRepository.findByParentAndStatusOrderByDisplayOrder(rootCategory, CategoryStatus.ACTIVE))
                    .willReturn(List.of(childCategory));

            // when
            List<Category> result = categoryDomainService.findByParent(rootCategory);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("CS_ALGO");
        }
    }

    @Nested
    @DisplayName("validateLeafCategory")
    class ValidateLeafCategoryTest {

        @Test
        @DisplayName("최하위 카테고리면 통과한다")
        void validateLeafCategory_withLeaf_success() {
            // given
            given(categoryRepository.findByParentAndStatusOrderByDisplayOrder(childCategory, CategoryStatus.ACTIVE))
                    .willReturn(List.of());

            // when
            categoryDomainService.validateLeafCategory(childCategory);

            // then
            verify(categoryRepository).findByParentAndStatusOrderByDisplayOrder(childCategory, CategoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("하위 카테고리가 있으면 예외를 발생시킨다")
        void validateLeafCategory_withNonLeaf_throwsException() {
            // given
            given(categoryRepository.findByParentAndStatusOrderByDisplayOrder(rootCategory, CategoryStatus.ACTIVE))
                    .willReturn(List.of(childCategory));

            // when & then
            assertThatThrownBy(() -> categoryDomainService.validateLeafCategory(rootCategory))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_LEAF);
                    });
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTest {

        @Test
        @DisplayName("새 카테고리를 생성한다")
        void createCategory_savesAndReturnsCategory() {
            // given
            given(categoryRepository.existsByCode("NEW_CAT")).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> {
                Category saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 3L);
                return saved;
            });

            // when
            Category result = categoryDomainService.createCategory("NEW_CAT", "새 카테고리", null, 1);

            // then
            assertThat(result.getCode()).isEqualTo("NEW_CAT");
            assertThat(result.getName()).isEqualTo("새 카테고리");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("이미 존재하는 코드로 생성 시 예외를 발생시킨다")
        void createCategory_withExistingCode_throwsException() {
            // given
            given(categoryRepository.existsByCode("CS")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryDomainService.createCategory("CS", "중복", null, 1))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_CODE_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTest {

        @Test
        @DisplayName("카테고리 정보를 업데이트한다")
        void updateCategory_updatesCategory() {
            // given
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));

            // when
            Category result = categoryDomainService.updateCategory(ROOT_CATEGORY_ID, "CS", "컴퓨터 공학", 2);

            // then
            assertThat(result.getName()).isEqualTo("컴퓨터 공학");
            assertThat(result.getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("코드 변경 시 중복 체크를 수행한다")
        void updateCategory_withNewCode_checksForDuplicate() {
            // given
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));
            given(categoryRepository.existsByCode("CS_NEW")).willReturn(false);

            // when
            Category result = categoryDomainService.updateCategory(ROOT_CATEGORY_ID, "CS_NEW", "컴퓨터 과학", 1);

            // then
            assertThat(result.getCode()).isEqualTo("CS_NEW");
            verify(categoryRepository).existsByCode("CS_NEW");
        }

        @Test
        @DisplayName("이미 존재하는 코드로 변경 시 예외를 발생시킨다")
        void updateCategory_withExistingCode_throwsException() {
            // given
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));
            given(categoryRepository.existsByCode("ENGLISH")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryDomainService.updateCategory(ROOT_CATEGORY_ID, "ENGLISH", "영어", 1))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_CODE_ALREADY_EXISTS);
                    });
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTest {

        @Test
        @DisplayName("카테고리를 삭제한다")
        void deleteCategory_deletesCategory() {
            // given
            Category categoryToDelete = createCategory(3L, "TO_DELETE", "삭제할 카테고리", null, 1);
            Card cardInCategory = Card.builder()
                    .question("Q")
                    .answer("A")
                    .category(categoryToDelete)
                    .build();
            given(categoryRepository.findByIdAndStatus(3L, CategoryStatus.ACTIVE)).willReturn(Optional.of(categoryToDelete));
            given(cardRepository.findByCategoryAndStatus(categoryToDelete, CardStatus.ACTIVE)).willReturn(List.of(cardInCategory));

            // when
            categoryDomainService.deleteCategory(3L);

            // then
            assertThat(categoryToDelete.getStatus()).isEqualTo(CategoryStatus.DELETED);
            assertThat(categoryToDelete.getDeletedAt()).isNotNull();
            assertThat(cardInCategory.getStatus()).isEqualTo(CardStatus.DELETED);
            assertThat(cardInCategory.getDeletedAt()).isNotNull();
            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        @DisplayName("하위 카테고리가 있으면 삭제 시 예외를 발생시킨다")
        void deleteCategory_withChildren_throwsException() {
            // given
            rootCategory.addChild(childCategory);
            given(categoryRepository.findByIdAndStatus(ROOT_CATEGORY_ID, CategoryStatus.ACTIVE)).willReturn(Optional.of(rootCategory));

            // when & then
            assertThatThrownBy(() -> categoryDomainService.deleteCategory(ROOT_CATEGORY_ID))
                    .isInstanceOf(CategoryException.class)
                    .satisfies(exception -> {
                        CategoryException categoryException = (CategoryException) exception;
                        assertThat(categoryException.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_HAS_CHILDREN);
                    });
        }
    }

    @Nested
    @DisplayName("existsByCode")
    class ExistsByCodeTest {

        @Test
        @DisplayName("존재하는 코드면 true를 반환한다")
        void existsByCode_withExistingCode_returnsTrue() {
            // given
            given(categoryRepository.existsByCode("CS")).willReturn(true);

            // when
            boolean result = categoryDomainService.existsByCode("CS");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 코드면 false를 반환한다")
        void existsByCode_withNonExistentCode_returnsFalse() {
            // given
            given(categoryRepository.existsByCode("INVALID")).willReturn(false);

            // when
            boolean result = categoryDomainService.existsByCode("INVALID");

            // then
            assertThat(result).isFalse();
        }
    }
}
