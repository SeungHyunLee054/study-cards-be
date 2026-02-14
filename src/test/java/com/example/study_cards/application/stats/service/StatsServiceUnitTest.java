package com.example.study_cards.application.stats.service;

import com.example.study_cards.application.stats.dto.response.StatsResponse;
import com.example.study_cards.domain.card.repository.CardRepositoryCustom.CategoryCount;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class StatsServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyDomainService studyDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @InjectMocks
    private StatsService statsService;

    private User testUser;
    private Category testCategory;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testCategory = createTestCategory();
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        ReflectionTestUtils.setField(user, "streak", 5);
        return user;
    }

    private Category createTestCategory() {
        Category category = Category.builder()
                .code("CS")
                .name("컴퓨터 과학")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTest {

        @Test
        @DisplayName("통계 정보를 조회한다")
        void getStats_returnsStatsResponse() {
            // given
            LocalDate today = LocalDate.now();

            given(studyDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(5);
            given(studyDomainService.countTotalStudiedCards(testUser)).willReturn(50);
            given(cardDomainService.count()).willReturn(100L);
            given(studyDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(100L, 80L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(cardDomainService.countAllByCategory()).willReturn(List.of(new CategoryCount(CATEGORY_ID, "CS", 50L)));
            given(studyDomainService.countStudiedByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 30L)));
            given(studyDomainService.countLearningByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 10L)));
            given(studyDomainService.countDueByCategory(eq(testUser), any(LocalDate.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 5L)));
            given(studyDomainService.countMasteredByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 15L)));
            given(studyDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.DailyActivity(today, 10L, 8L)));

            // when
            StatsResponse result = statsService.getStats(testUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.overview()).isNotNull();
            assertThat(result.overview().dueToday()).isEqualTo(5);
            assertThat(result.overview().totalStudied()).isEqualTo(50);
            assertThat(result.overview().newCards()).isEqualTo(50);
            assertThat(result.overview().streak()).isEqualTo(5);
            assertThat(result.overview().accuracyRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("학습 기록이 없을 때 통계 정보를 조회한다")
        void getStats_withNoRecords_returnsEmptyStats() {
            // given
            given(studyDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(100L);
            given(studyDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(cardDomainService.countAllByCategory()).willReturn(List.of(new CategoryCount(CATEGORY_ID, "CS", 50L)));
            given(studyDomainService.countStudiedByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.countLearningByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.countDueByCategory(eq(testUser), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());
            given(studyDomainService.countMasteredByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            StatsResponse result = statsService.getStats(testUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.overview().dueToday()).isZero();
            assertThat(result.overview().totalStudied()).isZero();
            assertThat(result.overview().newCards()).isEqualTo(100);
            assertThat(result.overview().accuracyRate()).isZero();
        }

        @Test
        @DisplayName("덱 통계를 정확하게 계산한다")
        void getStats_calculatesDeckStats() {
            // given
            given(studyDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(100L);
            given(studyDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(cardDomainService.countAllByCategory()).willReturn(List.of(new CategoryCount(CATEGORY_ID, "CS", 100L)));
            given(studyDomainService.countStudiedByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 40L)));
            given(studyDomainService.countLearningByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 20L)));
            given(studyDomainService.countDueByCategory(eq(testUser), any(LocalDate.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 10L)));
            given(studyDomainService.countMasteredByCategory(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 30L)));
            given(studyDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            StatsResponse result = statsService.getStats(testUser);

            // then
            assertThat(result.deckStats()).hasSize(1);
            assertThat(result.deckStats().get(0).category()).isEqualTo("CS");
            assertThat(result.deckStats().get(0).newCount()).isEqualTo(60);
            assertThat(result.deckStats().get(0).learningCount()).isEqualTo(20);
            assertThat(result.deckStats().get(0).reviewCount()).isEqualTo(10);
            assertThat(result.deckStats().get(0).masteryRate()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("최근 7일간의 활동 통계를 조회한다")
        void getStats_returnsRecentActivity() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            given(studyDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(0L);
            given(studyDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(Collections.emptyList());
            given(cardDomainService.countAllByCategory()).willReturn(Collections.emptyList());
            given(studyDomainService.countStudiedByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.countLearningByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.countDueByCategory(eq(testUser), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());
            given(studyDomainService.countMasteredByCategory(testUser)).willReturn(Collections.emptyList());
            given(studyDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(List.of(
                            new StudyRecordRepositoryCustom.DailyActivity(today, 20L, 18L),
                            new StudyRecordRepositoryCustom.DailyActivity(yesterday, 15L, 12L)
                    ));

            // when
            StatsResponse result = statsService.getStats(testUser);

            // then
            assertThat(result.recentActivity()).hasSize(2);
            assertThat(result.recentActivity().get(0).date()).isEqualTo(today);
            assertThat(result.recentActivity().get(0).studied()).isEqualTo(20);
            assertThat(result.recentActivity().get(0).correct()).isEqualTo(18);
        }
    }
}
