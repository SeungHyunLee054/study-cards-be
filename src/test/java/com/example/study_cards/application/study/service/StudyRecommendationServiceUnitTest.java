package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.CategoryAccuracyResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService.ScoredRecord;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StudyRecommendationServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyDomainService studyDomainService;

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private AiGenerationService aiGenerationService;

    @InjectMocks
    private StudyRecommendationService studyRecommendationService;

    private User testUser;
    private Card testCard;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testCategory = Category.builder()
                .code("CS")
                .name("컴퓨터 과학")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testCard = Card.builder()
                .question("테스트 질문")
                .questionSub("부가 설명")
                .answer("테스트 답변")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(testCard, "id", 1L);
    }

    @Nested
    @DisplayName("getRecommendations")
    class GetRecommendationsTest {

        @Test
        @DisplayName("FREE 플랜은 AI 설명 없이 추천 반환")
        void getRecommendations_freePlan_noAiExplanation() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(false)
                    .nextReviewDate(LocalDate.now()).efFactor(2.0).build();

            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.FREE);
            given(studyDomainService.findPrioritizedDueRecords(testUser, 20))
                    .willReturn(List.of(new ScoredRecord(record, 500)));

            // when
            RecommendationResponse response = studyRecommendationService.getRecommendations(testUser, 20);

            // then
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.aiExplanation()).isNull();
            verify(aiGenerationService, never()).generateContent(anyString());
        }

        @Test
        @DisplayName("PRO 플랜은 AI 설명 포함하여 추천 반환")
        void getRecommendations_proPlan_withAiExplanation() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(false)
                    .nextReviewDate(LocalDate.now()).efFactor(1.5).build();

            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(studyDomainService.findPrioritizedDueRecords(testUser, 20))
                    .willReturn(List.of(new ScoredRecord(record, 1300)));
            given(studyDomainService.calculateCategoryAccuracy(testUser))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 10L, 7L, 70.0)));
            given(aiGenerationService.generateContent(anyString()))
                    .willReturn("컴퓨터 과학 카테고리에서 오답이 많습니다. 우선 복습하세요.");

            // when
            RecommendationResponse response = studyRecommendationService.getRecommendations(testUser, 20);

            // then
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.aiExplanation()).isNotNull();
            assertThat(response.recommendations().get(0).priorityScore()).isEqualTo(1300);
            verify(aiGenerationService).generateContent(anyString());
        }

        @Test
        @DisplayName("복습할 카드가 없으면 빈 목록 반환")
        void getRecommendations_noCards_returnsEmpty() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.FREE);
            given(studyDomainService.findPrioritizedDueRecords(testUser, 20))
                    .willReturn(List.of());

            // when
            RecommendationResponse response = studyRecommendationService.getRecommendations(testUser, 20);

            // then
            assertThat(response.recommendations()).isEmpty();
            assertThat(response.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("AI 설명 생성 실패 시 null 반환")
        void getRecommendations_aiFailure_returnsNullExplanation() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(false)
                    .nextReviewDate(LocalDate.now()).efFactor(2.0).build();

            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(studyDomainService.findPrioritizedDueRecords(testUser, 20))
                    .willReturn(List.of(new ScoredRecord(record, 500)));
            given(studyDomainService.calculateCategoryAccuracy(testUser))
                    .willReturn(List.of());
            given(aiGenerationService.generateContent(anyString()))
                    .willThrow(new RuntimeException("API 호출 실패"));

            // when
            RecommendationResponse response = studyRecommendationService.getRecommendations(testUser, 20);

            // then
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.aiExplanation()).isNull();
        }
    }

    @Nested
    @DisplayName("getCategoryAccuracy")
    class GetCategoryAccuracyTest {

        @Test
        @DisplayName("카테고리별 정답률을 반환한다")
        void getCategoryAccuracy_returnsAccuracyList() {
            // given
            given(studyDomainService.calculateCategoryAccuracy(testUser))
                    .willReturn(List.of(
                            new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 15L, 75.0),
                            new CategoryAccuracy(2L, "NET", "네트워크", 10L, 4L, 40.0)
                    ));

            // when
            List<CategoryAccuracyResponse> result = studyRecommendationService.getCategoryAccuracy(testUser);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).categoryCode()).isEqualTo("CS");
            assertThat(result.get(0).accuracy()).isEqualTo(75.0);
            assertThat(result.get(1).accuracy()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("학습 기록이 없으면 빈 목록 반환")
        void getCategoryAccuracy_noRecords_returnsEmpty() {
            // given
            given(studyDomainService.calculateCategoryAccuracy(testUser))
                    .willReturn(List.of());

            // when
            List<CategoryAccuracyResponse> result = studyRecommendationService.getCategoryAccuracy(testUser);

            // then
            assertThat(result).isEmpty();
        }
    }
}
