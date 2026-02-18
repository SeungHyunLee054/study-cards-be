package com.example.study_cards.application.ai.service;

import com.example.study_cards.application.ai.dto.request.GenerateUserCardRequest;
import com.example.study_cards.application.ai.dto.response.AiLimitResponse;
import com.example.study_cards.application.ai.dto.response.UserAiGenerationResponse;
import com.example.study_cards.domain.ai.exception.AiErrorCode;
import com.example.study_cards.domain.ai.exception.AiException;
import com.example.study_cards.domain.ai.repository.AiGenerationLogRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import com.example.study_cards.infra.redis.service.AiLimitService;
import com.example.study_cards.support.BaseUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserAiCardServiceUnitTest extends BaseUnitTest {

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private AiLimitService aiLimitService;

    @Mock
    private AiGenerationService aiGenerationService;

    @Mock
    private UserCardRepository userCardRepository;

    @Mock
    private AiGenerationLogRepository aiGenerationLogRepository;

    @Mock
    private CategoryDomainService categoryDomainService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserAiCardService userAiCardService;

    private User testUser;
    private Category testCategory;
    private Category jlptFallbackCategory;
    private GenerateUserCardRequest testRequest;

    private static final Long USER_ID = 1L;
    private static final String AI_RESPONSE = """
            [
              {
                "question": "REST API란 무엇인가?",
                "questionSub": null,
                "answer": "Representational State Transfer의 약자로 웹 서비스 아키텍처 스타일",
                "answerSub": "HTTP 메서드를 사용하여 리소스를 조작"
              },
              {
                "question": "HTTP GET 메서드의 역할은?",
                "questionSub": "HTTP 메서드 중 하나",
                "answer": "서버로부터 리소스를 조회하는 메서드",
                "answerSub": null
              }
            ]
            """;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", USER_ID);

        testCategory = Category.builder()
                .code("CS")
                .name("컴퓨터과학")
                .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        jlptFallbackCategory = Category.builder()
                .code("JN_MISC")
                .name("일본어 기타")
                .build();
        ReflectionTestUtils.setField(jlptFallbackCategory, "id", 2L);

        lenient().when(categoryDomainService.isLeafCategory(any(Category.class))).thenReturn(true);

        testRequest = fixtureMonkey.giveMeBuilder(GenerateUserCardRequest.class)
                .set("sourceText", "REST API는 웹 서비스를 위한 아키텍처 스타일입니다.")
                .set("categoryCode", "CS")
                .set("count", 2)
                .set("difficulty", "보통")
                .sample();
    }

    @Nested
    @DisplayName("generateCards")
    class GenerateCardsTest {

        @Test
        @DisplayName("PRO 플랜 사용자는 AI 카드 생성 성공")
        void generateCards_proPlan_success() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn(AI_RESPONSE);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> {
                List<UserCard> cards = invocation.getArgument(0);
                for (int i = 0; i < cards.size(); i++) {
                    ReflectionTestUtils.setField(cards.get(i), "id", (long) (i + 1));
                }
                return cards;
            });
            given(aiLimitService.getRemainingCount(USER_ID, SubscriptionPlan.PRO)).willReturn(29);

            // when
            UserAiGenerationResponse response = userAiCardService.generateCards(testUser, testRequest);

            // then
            assertThat(response.generatedCards()).hasSize(2);
            assertThat(response.count()).isEqualTo(2);
            assertThat(response.remainingLimit()).isEqualTo(29);
            assertThat(response.generatedCards().get(0).question()).isEqualTo("REST API란 무엇인가?");
            assertThat(response.generatedCards().get(0).aiGenerated()).isTrue();

            verify(userCardRepository).saveAll(anyList());
            verify(aiGenerationLogRepository).save(any());
        }

        @Test
        @DisplayName("FREE 플랜 사용자도 생성 가능 (평생 5회)")
        void generateCards_freePlan_success() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.FREE);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.FREE)).willReturn(true);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn(AI_RESPONSE);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> {
                List<UserCard> cards = invocation.getArgument(0);
                for (int i = 0; i < cards.size(); i++) {
                    ReflectionTestUtils.setField(cards.get(i), "id", (long) (i + 1));
                }
                return cards;
            });
            given(aiLimitService.getRemainingCount(USER_ID, SubscriptionPlan.FREE)).willReturn(4);

            // when
            UserAiGenerationResponse response = userAiCardService.generateCards(testUser, testRequest);

            // then
            assertThat(response.generatedCards()).hasSize(2);
            assertThat(response.remainingLimit()).isEqualTo(4);
        }

        @Test
        @DisplayName("생성 횟수 초과 시 예외 발생")
        void generateCards_limitExceeded_throwsException() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.FREE);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.FREE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userAiCardService.generateCards(testUser, testRequest))
                    .isInstanceOf(AiException.class)
                    .extracting(e -> ((AiException) e).getErrorCode())
                    .isEqualTo(AiErrorCode.GENERATION_LIMIT_EXCEEDED);

            verify(userCardRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("관리자는 생성 슬롯 제한을 우회한다")
        void generateCards_adminBypassesGenerationLimit() {
            // given
            User adminUser = createAdminUser();
            given(subscriptionDomainService.getEffectivePlan(adminUser)).willReturn(SubscriptionPlan.PRO);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn(AI_RESPONSE);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> {
                List<UserCard> cards = invocation.getArgument(0);
                for (int i = 0; i < cards.size(); i++) {
                    ReflectionTestUtils.setField(cards.get(i), "id", (long) (i + 1));
                }
                return cards;
            });

            // when
            UserAiGenerationResponse response = userAiCardService.generateCards(adminUser, testRequest);

            // then
            assertThat(response.generatedCards()).hasSize(2);
            assertThat(response.remainingLimit()).isEqualTo(Integer.MAX_VALUE);
            verify(aiLimitService, never()).tryAcquireSlot(anyLong(), any());
            verify(aiLimitService, never()).getRemainingCount(anyLong(), any());
        }

        @Test
        @DisplayName("AI API 호출 실패 시 예외 발생")
        void generateCards_aiApiFails_throwsException() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString()))
                    .willThrow(new RuntimeException("API 호출 실패"));

            // when & then
            assertThatThrownBy(() -> userAiCardService.generateCards(testUser, testRequest))
                    .isInstanceOf(AiException.class)
                    .extracting(e -> ((AiException) e).getErrorCode())
                    .isEqualTo(AiErrorCode.AI_GENERATION_FAILED);

            // 실패 시 슬롯 해제 확인
            verify(aiLimitService).releaseSlot(USER_ID, SubscriptionPlan.PRO);
            // 실패 로그가 저장되었는지 확인
            verify(aiGenerationLogRepository).save(any());
        }

        @Test
        @DisplayName("AI 응답 파싱 실패 시 예외 발생")
        void generateCards_invalidResponse_throwsException() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn("invalid json response");

            // when & then
            assertThatThrownBy(() -> userAiCardService.generateCards(testUser, testRequest))
                    .isInstanceOf(AiException.class)
                    .extracting(e -> ((AiException) e).getErrorCode())
                    .isEqualTo(AiErrorCode.INVALID_AI_RESPONSE);

            // 실패 시 슬롯 해제 확인
            verify(aiLimitService).releaseSlot(USER_ID, SubscriptionPlan.PRO);
        }

        @Test
        @DisplayName("마크다운 코드블록으로 감싼 응답도 파싱 가능")
        void generateCards_markdownWrapped_success() {
            // given
            String wrappedResponse = "```json\n" + AI_RESPONSE + "\n```";
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("CS")).willReturn(testCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn(wrappedResponse);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> {
                List<UserCard> cards = invocation.getArgument(0);
                for (int i = 0; i < cards.size(); i++) {
                    ReflectionTestUtils.setField(cards.get(i), "id", (long) (i + 1));
                }
                return cards;
            });
            given(aiLimitService.getRemainingCount(USER_ID, SubscriptionPlan.PRO)).willReturn(29);

            // when
            UserAiGenerationResponse response = userAiCardService.generateCards(testUser, testRequest);

            // then
            assertThat(response.generatedCards()).hasSize(2);
        }

        @Test
        @DisplayName("카테고리와 입력 텍스트가 불일치하면 언어별 기타 카테고리로 폴백한다")
        @SuppressWarnings("unchecked")
        void generateCards_categoryMismatch_fallbackToLanguageMisc() {
            // given
            Category jlptCategory = Category.builder()
                    .code("JN_N3")
                    .name("일본어 > JLPT > N3")
                    .build();
            ReflectionTestUtils.setField(jlptCategory, "id", 3L);

            GenerateUserCardRequest mismatchRequest = fixtureMonkey.giveMeBuilder(GenerateUserCardRequest.class)
                    .set("sourceText", "운영체제 스케줄링과 프로세스 상태 전이에 대해 정리해줘.")
                    .set("categoryCode", "JN_N3")
                    .set("count", 2)
                    .set("difficulty", "보통")
                    .sample();

            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("JN_N3")).willReturn(jlptCategory);
            given(categoryDomainService.findByCodeOrNull("JN_MISC")).willReturn(jlptFallbackCategory);
            given(aiGenerationService.generateContent(anyString())).willReturn(AI_RESPONSE);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
            given(aiLimitService.getRemainingCount(USER_ID, SubscriptionPlan.PRO)).willReturn(29);

            // when
            userAiCardService.generateCards(testUser, mismatchRequest);

            // then
            ArgumentCaptor<List<UserCard>> captor = ArgumentCaptor.forClass(List.class);
            verify(userCardRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).allSatisfy(card ->
                    assertThat(card.getCategory().getCode()).isEqualTo("JN_MISC"));
        }

        @Test
        @DisplayName("상위 카테고리를 선택하면 leaf 기타 카테고리로 매핑한다")
        @SuppressWarnings("unchecked")
        void generateCards_nonLeafCategory_mapsToLeafMisc() {
            // given
            Category englishRoot = Category.builder()
                    .code("EN")
                    .name("영어")
                    .build();
            ReflectionTestUtils.setField(englishRoot, "id", 10L);

            Category englishMisc = Category.builder()
                    .code("EN_MISC")
                    .name("영어 기타")
                    .build();
            ReflectionTestUtils.setField(englishMisc, "id", 11L);

            GenerateUserCardRequest nonLeafRequest = fixtureMonkey.giveMeBuilder(GenerateUserCardRequest.class)
                    .set("sourceText", "Review this contract clause and choose the best word.")
                    .set("categoryCode", "EN")
                    .set("count", 2)
                    .set("difficulty", "보통")
                    .sample();

            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.tryAcquireSlot(USER_ID, SubscriptionPlan.PRO)).willReturn(true);
            given(categoryDomainService.findByCode("EN")).willReturn(englishRoot);
            given(categoryDomainService.isLeafCategory(englishRoot)).willReturn(false);
            given(categoryDomainService.findByCodeOrNull("EN_MISC")).willReturn(englishMisc);
            given(categoryDomainService.isLeafCategory(englishMisc)).willReturn(true);
            given(aiGenerationService.generateContent(anyString())).willReturn(AI_RESPONSE);
            given(userCardRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
            given(aiLimitService.getRemainingCount(USER_ID, SubscriptionPlan.PRO)).willReturn(29);

            // when
            userAiCardService.generateCards(testUser, nonLeafRequest);

            // then
            ArgumentCaptor<List<UserCard>> captor = ArgumentCaptor.forClass(List.class);
            verify(userCardRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).allSatisfy(card ->
                    assertThat(card.getCategory().getCode()).isEqualTo("EN_MISC"));
        }
    }

    @Nested
    @DisplayName("getGenerationLimit")
    class GetGenerationLimitTest {

        @Test
        @DisplayName("FREE 플랜의 제한 정보 조회")
        void getGenerationLimit_freePlan() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.FREE);
            given(aiLimitService.getUsedCount(USER_ID, SubscriptionPlan.FREE)).willReturn(3);

            // when
            AiLimitResponse response = userAiCardService.getGenerationLimit(testUser);

            // then
            assertThat(response.limit()).isEqualTo(5);
            assertThat(response.used()).isEqualTo(3);
            assertThat(response.remaining()).isEqualTo(2);
            assertThat(response.isLifetime()).isTrue();
        }

        @Test
        @DisplayName("PRO 플랜의 제한 정보 조회")
        void getGenerationLimit_proPlan() {
            // given
            given(subscriptionDomainService.getEffectivePlan(testUser)).willReturn(SubscriptionPlan.PRO);
            given(aiLimitService.getUsedCount(USER_ID, SubscriptionPlan.PRO)).willReturn(10);

            // when
            AiLimitResponse response = userAiCardService.getGenerationLimit(testUser);

            // then
            assertThat(response.limit()).isEqualTo(30);
            assertThat(response.used()).isEqualTo(10);
            assertThat(response.remaining()).isEqualTo(20);
            assertThat(response.isLifetime()).isFalse();
        }

        @Test
        @DisplayName("관리자는 제한 정보를 무제한으로 반환한다")
        void getGenerationLimit_adminUnlimited() {
            // given
            User adminUser = createAdminUser();

            // when
            AiLimitResponse response = userAiCardService.getGenerationLimit(adminUser);

            // then
            assertThat(response.limit()).isEqualTo(Integer.MAX_VALUE);
            assertThat(response.used()).isZero();
            assertThat(response.remaining()).isEqualTo(Integer.MAX_VALUE);
            assertThat(response.isLifetime()).isFalse();
            verify(subscriptionDomainService, never()).getEffectivePlan(adminUser);
        }
    }

    private User createAdminUser() {
        User adminUser = User.builder()
                .email("admin@example.com")
                .password("password123")
                .nickname("관리자")
                .roles(Set.of(Role.ROLE_ADMIN))
                .build();
        ReflectionTestUtils.setField(adminUser, "id", 999L);
        return adminUser;
    }
}
