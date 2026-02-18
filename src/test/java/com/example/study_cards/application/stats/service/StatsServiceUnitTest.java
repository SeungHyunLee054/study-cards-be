package com.example.study_cards.application.stats.service;

import com.example.study_cards.application.stats.dto.response.StatsResponse;
import com.example.study_cards.application.study.service.StudyCategoryAggregationService;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class StatsServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyRecordDomainService studyRecordDomainService;

    @Mock
    private StudyCategoryAggregationService studyCategoryAggregationService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @Mock
    private UserCardDomainService userCardDomainService;

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

            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(5);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(50);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(10);
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(20L);
            given(studyRecordDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(100L, 80L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser))
                    .willReturn(Map.of("CS", 70L));
            given(studyRecordDomainService.countStudiedByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 30L)));
            given(studyRecordDomainService.countLearningByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 10L)));
            given(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 5L)));
            given(studyRecordDomainService.countMasteredByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 15L)));
            given(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.DailyActivity(today, 10L, 8L)));

            // when
            StatsResponse result = statsService.getStats(testUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.overview()).isNotNull();
            assertThat(result.overview().dueToday()).isEqualTo(5);
            assertThat(result.overview().totalStudied()).isEqualTo(60);
            assertThat(result.overview().newCards()).isEqualTo(60);
            assertThat(result.overview().streak()).isEqualTo(5);
            assertThat(result.overview().accuracyRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("학습 기록이 없을 때 통계 정보를 조회한다")
        void getStats_withNoRecords_returnsEmptyStats() {
            // given
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser))
                    .willReturn(Map.of("CS", 50L));
            given(studyRecordDomainService.countStudiedByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.countLearningByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());
            given(studyRecordDomainService.countMasteredByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
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
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(List.of(testCategory));
            given(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser))
                    .willReturn(Map.of("CS", 100L));
            given(studyRecordDomainService.countStudiedByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 40L)));
            given(studyRecordDomainService.countLearningByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 20L)));
            given(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 10L)));
            given(studyRecordDomainService.countMasteredByCategoryWithUserCards(testUser))
                    .willReturn(List.of(new StudyRecordRepositoryCustom.CategoryCount(CATEGORY_ID, "CS", 30L)));
            given(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
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

            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(0);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);
            given(cardDomainService.count()).willReturn(0L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalAndCorrect(testUser))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));

            given(categoryDomainService.findLeafCategories()).willReturn(Collections.emptyList());
            given(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser))
                    .willReturn(Collections.emptyMap());
            given(studyRecordDomainService.countStudiedByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.countLearningByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());
            given(studyRecordDomainService.countMasteredByCategoryWithUserCards(testUser)).willReturn(Collections.emptyList());
            given(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
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
