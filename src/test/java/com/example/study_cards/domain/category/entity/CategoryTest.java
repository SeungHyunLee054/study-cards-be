package com.example.study_cards.domain.category.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = Category.builder()
                .code("CS")
                .name("Computer Science")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(rootCategory, "id", 1L);

        childCategory = Category.builder()
                .code("ALGO")
                .name("Algorithm")
                .parent(rootCategory)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(childCategory, "id", 2L);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("루트 카테고리 생성 시 depth가 0으로 설정된다")
        void builder_rootCategory_depthIsZero() {
            // then
            assertThat(rootCategory.getDepth()).isEqualTo(0);
            assertThat(rootCategory.getParent()).isNull();
        }

        @Test
        @DisplayName("자식 카테고리 생성 시 depth가 부모 + 1로 설정된다")
        void builder_childCategory_depthIsParentPlusOne() {
            // then
            assertThat(childCategory.getDepth()).isEqualTo(1);
            assertThat(childCategory.getParent()).isEqualTo(rootCategory);
        }

        @Test
        @DisplayName("displayOrder 미지정 시 기본값 0이 설정된다")
        void builder_withoutDisplayOrder_defaultsToZero() {
            // when
            Category category = Category.builder()
                    .code("TEST")
                    .name("Test")
                    .build();

            // then
            assertThat(category.getDisplayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("카테고리 생성 시 상태는 ACTIVE이다")
        void builder_defaultStatus_active() {
            // then
            assertThat(rootCategory.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            assertThat(rootCategory.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("카테고리 정보를 업데이트한다")
        void update_changesCategoryInfo() {
            // when
            rootCategory.update("CS_NEW", "Computer Science New", 2);

            // then
            assertThat(rootCategory.getCode()).isEqualTo("CS_NEW");
            assertThat(rootCategory.getName()).isEqualTo("Computer Science New");
            assertThat(rootCategory.getDisplayOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("updateParent")
    class UpdateParentTest {

        @Test
        @DisplayName("부모 카테고리를 변경하면 depth가 재계산된다")
        void updateParent_changesDepth() {
            // given
            Category newParent = Category.builder()
                    .code("MATH")
                    .name("Math")
                    .parent(rootCategory)
                    .displayOrder(1)
                    .build();

            // when
            childCategory.updateParent(newParent);

            // then
            assertThat(childCategory.getParent()).isEqualTo(newParent);
            assertThat(childCategory.getDepth()).isEqualTo(2);
        }

        @Test
        @DisplayName("부모를 null로 변경하면 루트 카테고리가 된다")
        void updateParent_toNull_becomesRoot() {
            // when
            childCategory.updateParent(null);

            // then
            assertThat(childCategory.getParent()).isNull();
            assertThat(childCategory.getDepth()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("isRootCategory")
    class IsRootCategoryTest {

        @Test
        @DisplayName("부모가 없으면 루트 카테고리이다")
        void isRootCategory_noParent_returnsTrue() {
            // then
            assertThat(rootCategory.isRootCategory()).isTrue();
        }

        @Test
        @DisplayName("부모가 있으면 루트 카테고리가 아니다")
        void isRootCategory_hasParent_returnsFalse() {
            // then
            assertThat(childCategory.isRootCategory()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasChildren")
    class HasChildrenTest {

        @Test
        @DisplayName("자식이 없으면 false를 반환한다")
        void hasChildren_noChildren_returnsFalse() {
            // given
            Category category = Category.builder()
                    .code("EMPTY")
                    .name("Empty")
                    .displayOrder(1)
                    .build();

            // then
            assertThat(category.hasChildren()).isFalse();
        }

        @Test
        @DisplayName("자식이 있으면 true를 반환한다")
        void hasChildren_withChildren_returnsTrue() {
            // given
            Category parent = Category.builder()
                    .code("PARENT")
                    .name("Parent")
                    .displayOrder(1)
                    .build();

            Category child = Category.builder()
                    .code("CHILD")
                    .name("Child")
                    .displayOrder(1)
                    .build();

            parent.addChild(child);

            // then
            assertThat(parent.hasChildren()).isTrue();
        }

        @Test
        @DisplayName("삭제된 자식만 있으면 false를 반환한다")
        void hasChildren_onlyDeletedChildren_returnsFalse() {
            // given
            Category parent = Category.builder()
                    .code("PARENT2")
                    .name("Parent2")
                    .displayOrder(1)
                    .build();
            Category child = Category.builder()
                    .code("CHILD2")
                    .name("Child2")
                    .displayOrder(1)
                    .build();
            parent.addChild(child);
            child.delete();

            // then
            assertThat(parent.hasChildren()).isFalse();
        }
    }

    @Nested
    @DisplayName("addChild")
    class AddChildTest {

        @Test
        @DisplayName("자식을 추가하면 부모-자식 관계가 설정된다")
        void addChild_setsParentChildRelationship() {
            // given
            Category parent = Category.builder()
                    .code("PARENT")
                    .name("Parent")
                    .displayOrder(1)
                    .build();

            Category child = Category.builder()
                    .code("CHILD")
                    .name("Child")
                    .displayOrder(1)
                    .build();

            // when
            parent.addChild(child);

            // then
            assertThat(parent.getChildren()).contains(child);
            assertThat(child.getParent()).isEqualTo(parent);
            assertThat(child.getDepth()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("카테고리를 soft delete 처리한다")
        void delete_marksCategoryAsDeleted() {
            // when
            rootCategory.delete();

            // then
            assertThat(rootCategory.getStatus()).isEqualTo(CategoryStatus.DELETED);
            assertThat(rootCategory.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 삭제된 카테고리를 다시 삭제해도 deletedAt은 유지된다")
        void delete_whenAlreadyDeleted_keepsDeletedAt() {
            // given
            rootCategory.delete();
            var firstDeletedAt = rootCategory.getDeletedAt();

            // when
            rootCategory.delete();

            // then
            assertThat(rootCategory.getStatus()).isEqualTo(CategoryStatus.DELETED);
            assertThat(rootCategory.getDeletedAt()).isEqualTo(firstDeletedAt);
        }
    }
}
