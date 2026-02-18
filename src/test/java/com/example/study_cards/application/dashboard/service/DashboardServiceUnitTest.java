package com.example.study_cards.application.dashboard.service;

import com.example.study_cards.application.dashboard.dto.response.*;
import com.example.study_cards.application.study.service.StudyCategoryAggregationService;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryCount;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.DailyActivity;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TotalAndCorrect;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

class DashboardServiceUnitTest extends BaseUnitTest {

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
    private DashboardService dashboardService;

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
    @DisplayName("getDashboard")
    class GetDashboardTest {

        @Test
        @DisplayName("대시보드 정보를 조회한다")
        void getDashboard_success() {
            // given
            setupBasicMocks();

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result).isNotNull();
            assertThat(result.user()).isNotNull();
            assertThat(result.today()).isNotNull();
            assertThat(result.categoryProgress()).isNotNull();
            assertThat(result.recentActivity()).isNotNull();
            assertThat(result.recommendation()).isNotNull();
        }

        @Test
        @DisplayName("사용자 요약 정보를 올바르게 생성한다")
        void getDashboard_userSummary_isCorrect() {
            // given
            setupBasicMocks();
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(100);

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.user().id()).isEqualTo(USER_ID);
            assertThat(result.user().nickname()).isEqualTo("테스트유저");
            assertThat(result.user().streak()).isEqualTo(5);
            assertThat(result.user().totalStudied()).isEqualTo(100);
            assertThat(result.user().level()).isEqualTo(4);
        }

        @Test
        @DisplayName("오늘 학습 정보를 올바르게 생성한다")
        void getDashboard_todayStudyInfo_isCorrect() {
            // given
            setupBasicMocks();
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(10);
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(50);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);
            given(studyRecordDomainService.countTodayStudy(eq(testUser), any(LocalDate.class)))
                    .willReturn(new TotalAndCorrect(20L, 18L));

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.today().dueCards()).isEqualTo(10);
            assertThat(result.today().newCardsAvailable()).isEqualTo(50);
            assertThat(result.today().studiedToday()).isEqualTo(20);
            assertThat(result.today().todayAccuracy()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("복습 카드가 있으면 REVIEW 추천을 반환한다")
        void getDashboard_withDueCards_returnsReviewRecommendation() {
            // given
            setupBasicMocks();
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(5);
            given(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .willReturn(List.of(new CategoryCount(1L, "CS", 5L)));

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.recommendation().type()).isEqualTo(StudyRecommendation.RecommendationType.REVIEW);
            assertThat(result.recommendation().cardsToStudy()).isEqualTo(5);
        }

        @Test
        @DisplayName("스트릭이 있고 오늘 학습이 없으면 STREAK_KEEP 추천을 반환한다")
        void getDashboard_withStreakAndNoStudy_returnsStreakKeepRecommendation() {
            // given
            setupBasicMocks();
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTodayStudy(eq(testUser), any(LocalDate.class)))
                    .willReturn(new TotalAndCorrect(0L, 0L));
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(50);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.recommendation().type()).isEqualTo(StudyRecommendation.RecommendationType.STREAK_KEEP);
        }

        @Test
        @DisplayName("새 카드가 있으면 NEW 추천을 반환한다")
        void getDashboard_withNewCards_returnsNewRecommendation() {
            // given
            ReflectionTestUtils.setField(testUser, "streak", 0);
            setupBasicMocks();
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTodayStudy(eq(testUser), any(LocalDate.class)))
                    .willReturn(new TotalAndCorrect(5L, 5L));
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(50);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.recommendation().type()).isEqualTo(StudyRecommendation.RecommendationType.NEW);
        }

        @Test
        @DisplayName("모든 카드를 학습했으면 COMPLETE 추천을 반환한다")
        void getDashboard_allCardsStudied_returnsCompleteRecommendation() {
            // given
            ReflectionTestUtils.setField(testUser, "streak", 0);
            setupBasicMocks();
            given(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).willReturn(0);
            given(studyRecordDomainService.countTodayStudy(eq(testUser), any(LocalDate.class)))
                    .willReturn(new TotalAndCorrect(5L, 5L));
            given(cardDomainService.count()).willReturn(100L);
            given(userCardDomainService.countByUser(testUser)).willReturn(0L);
            given(studyRecordDomainService.countTotalStudiedCards(testUser)).willReturn(100);
            given(studyRecordDomainService.countTotalStudiedUserCards(testUser)).willReturn(0);

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.recommendation().type()).isEqualTo(StudyRecommendation.RecommendationType.COMPLETE);
        }

        @Test
        @DisplayName("최근 활동을 올바르게 조회한다")
        void getDashboard_recentActivity_isCorrect() {
            // given
            setupBasicMocks();
            LocalDate today = LocalDate.now();
            given(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .willReturn(List.of(
                            new DailyActivity(today, 10L, 8L),
                            new DailyActivity(today.minusDays(1), 15L, 12L)
                    ));

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.recentActivity()).hasSize(2);
            assertThat(result.recentActivity().get(0).date()).isEqualTo(today);
            assertThat(result.recentActivity().get(0).studied()).isEqualTo(10);
            assertThat(result.recentActivity().get(0).correct()).isEqualTo(8);
            assertThat(result.recentActivity().get(0).accuracy()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("카테고리 진행률을 전체 카테고리로 반환한다")
        void getDashboard_categoryProgress_returnsAllCategories() {
            // given
            setupBasicMocks();
            given(categoryDomainService.findAll()).willReturn(List.of(
                    createCategory("CS", 1),
                    createCategory("EN", 2),
                    createCategory("JP", 3),
                    createCategory("CN", 4),
                    createCategory("KR", 5),
                    createCategory("ES", 6)
            ));
            given(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser)).willReturn(Map.of(
                    "CS", 105L,
                    "EN", 90L,
                    "JP", 80L,
                    "CN", 70L,
                    "KR", 60L,
                    "ES", 50L
            ));

            // when
            DashboardResponse result = dashboardService.getDashboard(testUser);

            // then
            assertThat(result.categoryProgress()).hasSize(6);
        }

        private void setupBasicMocks() {
            lenient().when(studyRecordDomainService.countTotalStudiedCards(testUser)).thenReturn(50);
            lenient().when(studyRecordDomainService.countTotalStudiedUserCards(testUser)).thenReturn(0);
            lenient().when(studyRecordDomainService.countDueCards(eq(testUser), any(LocalDate.class))).thenReturn(0);
            lenient().when(cardDomainService.count()).thenReturn(100L);
            lenient().when(userCardDomainService.countByUser(testUser)).thenReturn(0L);
            lenient().when(studyRecordDomainService.countTodayStudy(eq(testUser), any(LocalDate.class)))
                    .thenReturn(new TotalAndCorrect(10L, 8L));
            lenient().when(categoryDomainService.findAll()).thenReturn(List.of(testCategory));
            lenient().when(studyCategoryAggregationService.countTotalCardsByCategoryWithUserCards(testUser))
                    .thenReturn(Map.of("CS", 100L));
            lenient().when(studyRecordDomainService.countStudiedByCategoryWithUserCards(testUser))
                    .thenReturn(List.of(new CategoryCount(1L, "CS", 50L)));
            lenient().when(studyRecordDomainService.countMasteredByCategoryWithUserCards(testUser))
                    .thenReturn(List.of(new CategoryCount(1L, "CS", 20L)));
            lenient().when(studyRecordDomainService.countDueByCategoryWithUserCards(eq(testUser), any(LocalDate.class)))
                    .thenReturn(List.of());
            lenient().when(studyRecordDomainService.findDailyActivity(eq(testUser), any(LocalDateTime.class)))
                    .thenReturn(List.of());
        }

        private Category createCategory(String code, int order) {
            Category category = Category.builder()
                    .code(code)
                    .name(code + " 카테고리")
                    .displayOrder(order)
                    .build();
            ReflectionTestUtils.setField(category, "id", (long) order);
            return category;
        }
    }
}
