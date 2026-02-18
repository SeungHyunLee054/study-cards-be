package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.AiRecommendationResponse;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.service.AiGenerationLogDomainService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.model.CategoryAccuracy;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.TotalAndCorrect;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.study.service.StudyRecordDomainService.ScoredRecord;
import com.example.study_cards.domain.subscription.entity.BillingCycle;
import com.example.study_cards.domain.subscription.entity.Subscription;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.entity.SubscriptionStatus;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.infra.redis.service.AiReviewQuotaService;
import com.example.study_cards.support.BaseUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StudyAiRecommendationServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyRecordDomainService studyRecordDomainService;
    @Mock
    private SubscriptionDomainService subscriptionDomainService;
    @Mock
    private AiReviewQuotaService aiReviewQuotaService;
    @Mock
    private AiGenerationService aiGenerationService;
    @Mock
    private AiGenerationLogDomainService aiGenerationLogDomainService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StudyAiRecommendationService studyAiRecommendationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(studyAiRecommendationService, "minStudiedCards", 1);
        ReflectionTestUtils.setField(studyAiRecommendationService, "minRecommendationCards", 1);
    }

    @Nested
    @DisplayName("getAiRecommendations")
    class GetAiRecommendationsTest {

        @Test
        @DisplayName("FREE 플랜은 AI 추천을 사용할 수 없다")
        void getAiRecommendations_freePlan_throwsForbidden() {
            // given
            User user = createUser();
            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.FREE);

            // when & then
            assertThatThrownBy(() -> studyAiRecommendationService.getAiRecommendations(user, 20))
                    .isInstanceOf(AiException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.AI_FEATURE_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("PRO 플랜은 AI 추천 응답을 반환한다")
        void getAiRecommendations_proPlan_returnsAiResult() throws Exception {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 10L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(user))
                    .willReturn(new TotalAndCorrect(20L, 10L));
            given(aiReviewQuotaService.tryAcquireSlot(anyLong(), any())).willReturn(true);
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 1, 99, LocalDateTime.now().plusDays(20)));
            given(aiGenerationService.generateContent(any()))
                    .willReturn("""
                            {
                              "weakConcepts":[{"concept":"운영체제","reason":"정답률이 낮습니다."}],
                              "reviewStrategy":"운영체제 개념을 10분 복습한 뒤 추천 카드를 풀어보세요."
                            }
                            """);
            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isTrue();
            assertThat(response.algorithmFallback()).isFalse();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.NONE);
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.weakConcepts()).isNotEmpty();
            assertThat(response.reviewStrategy()).isNotBlank();
        }

        @Test
        @DisplayName("한도 초과 시 알고리즘 폴백 응답을 반환한다")
        void getAiRecommendations_whenQuotaExceeded_returnsFallback() {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 10L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(user))
                    .willReturn(new TotalAndCorrect(20L, 10L));
            given(aiReviewQuotaService.tryAcquireSlot(anyLong(), any())).willReturn(false);
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 100, 0, LocalDateTime.now().plusDays(20)));

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isFalse();
            assertThat(response.algorithmFallback()).isTrue();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.QUOTA_EXCEEDED);
            assertThat(response.recommendations()).hasSize(1);
            assertThat(response.quota().remaining()).isZero();
        }

        @Test
        @DisplayName("관리자는 플랜/쿼터 제한을 우회하고 AI 추천을 사용한다")
        void getAiRecommendations_adminBypassesQuotaAndSubscription() {
            // given
            User adminUser = createAdminUser();

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(adminUser)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(adminUser)).willReturn(SubscriptionPlan.FREE);
            given(studyRecordDomainService.findPrioritizedDueRecords(adminUser, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(adminUser))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 10L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(adminUser))
                    .willReturn(new TotalAndCorrect(20L, 10L));
            given(aiGenerationService.generateContent(any()))
                    .willReturn("""
                            {
                              "weakConcepts":[{"concept":"운영체제","reason":"정답률이 낮습니다."}],
                              "reviewStrategy":"운영체제 개념을 10분 복습한 뒤 추천 카드를 풀어보세요."
                            }
                            """);

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(adminUser, 20);

            // then
            assertThat(response.aiUsed()).isTrue();
            assertThat(response.algorithmFallback()).isFalse();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.NONE);
            assertThat(response.quota().limit()).isEqualTo(Integer.MAX_VALUE);
            assertThat(response.quota().remaining()).isEqualTo(Integer.MAX_VALUE);
            verify(subscriptionDomainService, never()).getSubscription(adminUser.getId());
            verify(aiReviewQuotaService, never()).tryAcquireSlot(anyLong(), any());
            verify(aiReviewQuotaService, never()).getQuota(anyLong(), any());
        }

        @Test
        @DisplayName("AI 응답 파싱 실패 시 알고리즘 폴백으로 응답한다")
        void getAiRecommendations_invalidAiResponse_returnsFallback() {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 10L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(user))
                    .willReturn(new TotalAndCorrect(20L, 10L));
            given(aiReviewQuotaService.tryAcquireSlot(anyLong(), any())).willReturn(true);
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 2, 98, LocalDateTime.now().plusDays(20)));
            given(aiGenerationService.generateContent(any())).willReturn("not-json-response");

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isFalse();
            assertThat(response.algorithmFallback()).isTrue();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.AI_ERROR);
            assertThat(response.reviewStrategy()).isNotBlank();
            verify(aiReviewQuotaService).releaseSlot(user.getId(), subscription);
        }

        @Test
        @DisplayName("AI 호출 예외 발생 시 쿼터 슬롯을 해제하고 폴백 응답한다")
        void getAiRecommendations_aiFailure_releasesQuotaAndReturnsFallback() {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 20L, 10L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(user))
                    .willReturn(new TotalAndCorrect(20L, 10L));
            given(aiReviewQuotaService.tryAcquireSlot(anyLong(), any())).willReturn(true);
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 3, 97, LocalDateTime.now().plusDays(20)));
            given(aiGenerationService.generateContent(any())).willThrow(new RuntimeException("AI timeout"));

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isFalse();
            assertThat(response.algorithmFallback()).isTrue();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.AI_ERROR);
            verify(aiReviewQuotaService).releaseSlot(user.getId(), subscription);
        }

        @Test
        @DisplayName("추천 카드가 없으면 AI를 호출하지 않고 규칙 기반 폴백을 반환한다")
        void getAiRecommendations_noRecommendations_returnsFallbackWithoutAiCall() {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20)).willReturn(List.of());
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 10L, 5L, 50.0)));
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 10, 90, LocalDateTime.now().plusDays(20)));

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isFalse();
            assertThat(response.algorithmFallback()).isFalse();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.NO_DUE_CARDS);
            assertThat(response.recommendations()).isEmpty();
            verify(aiReviewQuotaService, never()).tryAcquireSlot(anyLong(), any());
            verify(aiGenerationService, never()).generateContent(any());
            verify(aiGenerationLogDomainService).save(any());
        }

        @Test
        @DisplayName("학습량이 부족하면 AI 호출 없이 폴백하고 히스토리를 저장한다")
        void getAiRecommendations_insufficientStudyData_returnsFallbackWithoutAiCall() {
            // given
            User user = createUser();
            Subscription subscription = createSubscription(user);
            ReflectionTestUtils.setField(studyAiRecommendationService, "minStudiedCards", 10);

            Card card = createCard();
            StudyRecord record = StudyRecord.builder()
                    .user(user)
                    .card(card)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(1.8)
                    .build();

            given(subscriptionDomainService.getEffectivePlan(user)).willReturn(SubscriptionPlan.PRO);
            given(subscriptionDomainService.getSubscription(user.getId())).willReturn(subscription);
            given(studyRecordDomainService.findPrioritizedDueRecords(user, 20))
                    .willReturn(List.of(new ScoredRecord(record, 900)));
            given(studyRecordDomainService.calculateCategoryAccuracy(user))
                    .willReturn(List.of(new CategoryAccuracy(1L, "CS", "컴퓨터 과학", 2L, 1L, 50.0)));
            given(studyRecordDomainService.countTotalAndCorrect(user))
                    .willReturn(new TotalAndCorrect(2L, 1L));
            given(aiReviewQuotaService.getQuota(anyLong(), any()))
                    .willReturn(new AiReviewQuotaService.ReviewQuota(100, 10, 90, LocalDateTime.now().plusDays(20)));

            // when
            AiRecommendationResponse response = studyAiRecommendationService.getAiRecommendations(user, 20);

            // then
            assertThat(response.aiUsed()).isFalse();
            assertThat(response.algorithmFallback()).isFalse();
            assertThat(response.fallbackReason()).isEqualTo(AiRecommendationResponse.FallbackReason.INSUFFICIENT_STUDY_DATA);
            verify(aiReviewQuotaService, never()).tryAcquireSlot(anyLong(), any());
            verify(aiGenerationService, never()).generateContent(any());
            verify(aiGenerationLogDomainService).save(any());
        }
    }

    private User createUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private User createAdminUser() {
        User adminUser = User.builder()
                .email("admin@example.com")
                .password("password")
                .nickname("admin")
                .roles(Set.of(Role.ROLE_ADMIN))
                .build();
        ReflectionTestUtils.setField(adminUser, "id", 999L);
        return adminUser;
    }

    private Subscription createSubscription(User user) {
        return Subscription.builder()
                .user(user)
                .plan(SubscriptionPlan.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .build();
    }

    private Card createCard() {
        Category category = Category.builder()
                .code("CS")
                .name("컴퓨터 과학")
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(category, "id", 1L);

        Card card = Card.builder()
                .question("CPU 스케줄링이란?")
                .answer("프로세스에 CPU를 할당하는 정책")
                .category(category)
                .efFactor(2.5)
                .build();
        ReflectionTestUtils.setField(card, "id", 1L);
        return card;
    }
}
