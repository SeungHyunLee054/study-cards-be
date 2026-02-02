package com.example.study_cards.application.stats.service;

import com.example.study_cards.application.stats.dto.response.DailyActivity;
import com.example.study_cards.application.stats.dto.response.DeckStats;
import com.example.study_cards.application.stats.dto.response.StatsResponse;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.card.repository.CardRepositoryCustom.CategoryCount;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class StatsServiceUnitTest extends BaseUnitTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @Mock
    private CardRepository cardRepository;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("testUser")
                .build();

        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);
    }

    @Nested
    @DisplayName("getStats")
    class GetStatsTest {

        @Test
        @DisplayName("통계를 정상적으로 조회한다")
        void getStats_success() {
            // given
            LocalDate today = LocalDate.now();
            given(studyRecordRepository.countDueCards(eq(user), any(LocalDate.class))).willReturn(5);
            given(studyRecordRepository.countTotalStudiedCards(user)).willReturn(10);
            given(cardRepository.count()).willReturn(100L);
            given(studyRecordRepository.countTotalAndCorrect(user))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(20L, 17L));
            given(cardRepository.countByCategory()).willReturn(List.of(
                    new CategoryCount(Category.CS, 50L),
                    new CategoryCount(Category.ENGLISH, 30L)
            ));
            given(studyRecordRepository.countStudiedByCategory(user)).willReturn(List.of(
                    new StudyRecordRepositoryCustom.CategoryCount(Category.CS, 5L)
            ));
            given(studyRecordRepository.countLearningByCategory(user)).willReturn(List.of(
                    new StudyRecordRepositoryCustom.CategoryCount(Category.CS, 3L)
            ));
            given(studyRecordRepository.countDueByCategory(eq(user), any(LocalDate.class))).willReturn(List.of(
                    new StudyRecordRepositoryCustom.CategoryCount(Category.CS, 2L)
            ));
            given(studyRecordRepository.countMasteredByCategory(user)).willReturn(List.of(
                    new StudyRecordRepositoryCustom.CategoryCount(Category.CS, 5L)
            ));
            given(studyRecordRepository.findDailyActivity(eq(user), any(LocalDateTime.class))).willReturn(List.of(
                    new StudyRecordRepositoryCustom.DailyActivity(today, 5L, 4L),
                    new StudyRecordRepositoryCustom.DailyActivity(today.minusDays(1), 3L, 2L)
            ));

            // when
            StatsResponse result = statsService.getStats(user);

            // then
            assertThat(result).isNotNull();
            assertThat(result.overview().dueToday()).isEqualTo(5);
            assertThat(result.overview().totalStudied()).isEqualTo(10);
            assertThat(result.overview().newCards()).isEqualTo(90);
            assertThat(result.overview().streak()).isEqualTo(0);
            assertThat(result.overview().accuracyRate()).isEqualTo(85.0);

            DeckStats csStats = result.deckStats().stream()
                    .filter(d -> d.category().equals("CS"))
                    .findFirst()
                    .orElseThrow();
            assertThat(csStats.masteryRate()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("학습 기록이 없으면 모든 카드가 new로 표시된다")
        void getStats_noStudyRecord_allCardsAreNew() {
            // given
            given(studyRecordRepository.countDueCards(eq(user), any(LocalDate.class))).willReturn(0);
            given(studyRecordRepository.countTotalStudiedCards(user)).willReturn(0);
            given(cardRepository.count()).willReturn(100L);
            given(studyRecordRepository.countTotalAndCorrect(user))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));
            given(cardRepository.countByCategory()).willReturn(List.of(
                    new CategoryCount(Category.CS, 100L)
            ));
            given(studyRecordRepository.countStudiedByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countLearningByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countDueByCategory(eq(user), any(LocalDate.class))).willReturn(Collections.emptyList());
            given(studyRecordRepository.countMasteredByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.findDailyActivity(eq(user), any(LocalDateTime.class))).willReturn(Collections.emptyList());

            // when
            StatsResponse result = statsService.getStats(user);

            // then
            assertThat(result.overview().newCards()).isEqualTo(100);
            assertThat(result.overview().totalStudied()).isEqualTo(0);
            assertThat(result.overview().accuracyRate()).isEqualTo(0.0);

            DeckStats csStats = result.deckStats().stream()
                    .filter(d -> d.category().equals("CS"))
                    .findFirst()
                    .orElseThrow();
            assertThat(csStats.newCount()).isEqualTo(100);
            assertThat(csStats.learningCount()).isEqualTo(0);
            assertThat(csStats.reviewCount()).isEqualTo(0);
            assertThat(csStats.masteryRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("모든 카테고리에 대한 DeckStats를 반환한다")
        void getStats_returnsAllCategoryDeckStats() {
            // given
            given(studyRecordRepository.countDueCards(eq(user), any(LocalDate.class))).willReturn(0);
            given(studyRecordRepository.countTotalStudiedCards(user)).willReturn(0);
            given(cardRepository.count()).willReturn(0L);
            given(studyRecordRepository.countTotalAndCorrect(user))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));
            given(cardRepository.countByCategory()).willReturn(Collections.emptyList());
            given(studyRecordRepository.countStudiedByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countLearningByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countDueByCategory(eq(user), any(LocalDate.class))).willReturn(Collections.emptyList());
            given(studyRecordRepository.countMasteredByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.findDailyActivity(eq(user), any(LocalDateTime.class))).willReturn(Collections.emptyList());

            // when
            StatsResponse result = statsService.getStats(user);

            // then
            assertThat(result.deckStats()).hasSize(Category.values().length);
        }

        @Test
        @DisplayName("최근 7일간의 활동 기록을 반환한다")
        void getStats_returnsRecentActivity() {
            // given
            LocalDate today = LocalDate.now();
            given(studyRecordRepository.countDueCards(eq(user), any(LocalDate.class))).willReturn(0);
            given(studyRecordRepository.countTotalStudiedCards(user)).willReturn(0);
            given(cardRepository.count()).willReturn(0L);
            given(studyRecordRepository.countTotalAndCorrect(user))
                    .willReturn(new StudyRecordRepositoryCustom.TotalAndCorrect(0L, 0L));
            given(cardRepository.countByCategory()).willReturn(Collections.emptyList());
            given(studyRecordRepository.countStudiedByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countLearningByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.countDueByCategory(eq(user), any(LocalDate.class))).willReturn(Collections.emptyList());
            given(studyRecordRepository.countMasteredByCategory(user)).willReturn(Collections.emptyList());
            given(studyRecordRepository.findDailyActivity(eq(user), any(LocalDateTime.class))).willReturn(List.of(
                    new StudyRecordRepositoryCustom.DailyActivity(today, 10L, 8L),
                    new StudyRecordRepositoryCustom.DailyActivity(today.minusDays(1), 5L, 4L)
            ));

            // when
            StatsResponse result = statsService.getStats(user);

            // then
            assertThat(result.recentActivity()).hasSize(2);
            DailyActivity todayActivity = result.recentActivity().get(0);
            assertThat(todayActivity.date()).isEqualTo(today);
            assertThat(todayActivity.studied()).isEqualTo(10);
            assertThat(todayActivity.correct()).isEqualTo(8);
        }
    }
}
