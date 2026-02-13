package com.example.study_cards.application.study.service;

import com.example.study_cards.application.card.dto.response.CardType;
import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.SessionResponse;
import com.example.study_cards.application.study.dto.response.SessionStatsResponse;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService.StudyCardItem;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class StudyServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyDomainService studyDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @Mock
    private UserCardDomainService userCardDomainService;

    @Mock
    private CategoryDomainService categoryDomainService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StudyService studyService;

    private User testUser;
    private Card testCard;
    private UserCard testUserCard;
    private Category testCategory;
    private StudyRecord testRecord;
    private StudySession testSession;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;
    private static final Long USER_CARD_ID = 2L;
    private static final Long CATEGORY_ID = 1L;
    private static final Long SESSION_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testCategory = createTestCategory();
        testCard = createTestCard();
        testUserCard = createTestUserCard();
        testSession = createTestSession();
        testRecord = createTestRecord();
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
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

    private Card createTestCard() {
        Card card = Card.builder()
                .question("테스트 질문")
                .questionSub("Test Question")
                .answer("테스트 답변")
                .answerSub("Test Answer")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        return card;
    }

    private StudySession createTestSession() {
        StudySession session = StudySession.builder()
                .user(testUser)
                .build();
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        return session;
    }

    private UserCard createTestUserCard() {
        UserCard userCard = UserCard.builder()
                .user(testUser)
                .question("사용자 질문")
                .questionSub("User Question")
                .answer("사용자 답변")
                .answerSub("User Answer")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(userCard, "id", USER_CARD_ID);
        return userCard;
    }

    private StudyRecord createTestRecord() {
        StudyRecord record = StudyRecord.builder()
                .user(testUser)
                .card(testCard)
                .session(testSession)
                .isCorrect(true)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .interval(1)
                .efFactor(2.6)
                .build();
        ReflectionTestUtils.setField(record, "id", 1L);
        return record;
    }

    @Nested
    @DisplayName("getTodayCards")
    class GetTodayCardsTest {

        private Pageable pageable;

        @BeforeEach
        void setUpPageable() {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "efFactor"));
        }

        @Test
        @DisplayName("카테고리 코드로 오늘의 학습 카드를 페이지네이션하여 조회한다")
        void getTodayCards_withCategoryCode_returnsCards() {
            // given
            given(categoryDomainService.findByCodeOrNull("CS")).willReturn(testCategory);
            given(studyDomainService.findTodayAllStudyCards(eq(testUser), eq(testCategory), anyInt()))
                    .willReturn(List.of(StudyCardItem.ofCard(testCard)));

            // when
            Page<StudyCardResponse> result = studyService.getTodayCards(testUser, "CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(CARD_ID);
            assertThat(result.getContent().get(0).question()).isEqualTo("테스트 질문");
            assertThat(result.getContent().get(0).cardType()).isEqualTo(CardType.PUBLIC);
        }

        @Test
        @DisplayName("카테고리 없이 모든 학습 카드를 페이지네이션하여 조회한다")
        void getTodayCards_withoutCategoryCode_returnsAllCards() {
            // given
            given(studyDomainService.findTodayAllStudyCards(eq(testUser), isNull(), anyInt()))
                    .willReturn(List.of(StudyCardItem.ofCard(testCard)));

            // when
            Page<StudyCardResponse> result = studyService.getTodayCards(testUser, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("UserCard와 Card가 혼합되어 반환된다")
        void getTodayCards_withMixedCards_returnsUserCardFirst() {
            // given
            given(categoryDomainService.findByCodeOrNull("CS")).willReturn(testCategory);
            given(studyDomainService.findTodayAllStudyCards(eq(testUser), eq(testCategory), anyInt()))
                    .willReturn(List.of(
                            StudyCardItem.ofUserCard(testUserCard),
                            StudyCardItem.ofCard(testCard)
                    ));

            // when
            Page<StudyCardResponse> result = studyService.getTodayCards(testUser, "CS", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).cardType()).isEqualTo(CardType.CUSTOM);
            assertThat(result.getContent().get(1).cardType()).isEqualTo(CardType.PUBLIC);
        }
    }

    @Nested
    @DisplayName("submitAnswer")
    class SubmitAnswerTest {

        @Test
        @DisplayName("공용카드 정답을 제출하고 결과를 반환한다")
        void submitAnswer_withPublicCard_returnsResult() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, CardType.PUBLIC, true);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));
            given(studyDomainService.processAnswer(eq(testUser), eq(testCard), eq(testSession), eq(true)))
                    .willReturn(testRecord);

            // when
            StudyResultResponse result = studyService.submitAnswer(testUser, request);

            // then
            assertThat(result.cardId()).isEqualTo(CARD_ID);
            assertThat(result.cardType()).isEqualTo(CardType.PUBLIC);
            assertThat(result.isCorrect()).isTrue();
            assertThat(result.nextReviewDate()).isEqualTo(testRecord.getNextReviewDate());
            assertThat(result.newEfFactor()).isEqualTo(testRecord.getEfFactor());
        }

        @Test
        @DisplayName("개인카드 정답을 제출하고 결과를 반환한다")
        void submitAnswer_withCustomCard_returnsResult() {
            // given
            StudyRecord userCardRecord = StudyRecord.builder()
                    .user(testUser)
                    .userCard(testUserCard)
                    .session(testSession)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.6)
                    .build();
            ReflectionTestUtils.setField(userCardRecord, "id", 2L);

            StudyAnswerRequest request = new StudyAnswerRequest(USER_CARD_ID, CardType.CUSTOM, true);
            given(userCardDomainService.findById(USER_CARD_ID)).willReturn(testUserCard);
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));
            given(studyDomainService.processUserCardAnswer(eq(testUser), eq(testUserCard), eq(testSession), eq(true)))
                    .willReturn(userCardRecord);

            // when
            StudyResultResponse result = studyService.submitAnswer(testUser, request);

            // then
            assertThat(result.cardId()).isEqualTo(USER_CARD_ID);
            assertThat(result.cardType()).isEqualTo(CardType.CUSTOM);
            assertThat(result.isCorrect()).isTrue();
        }

        @Test
        @DisplayName("활성 세션이 없으면 새 세션을 생성한다")
        void submitAnswer_withNoActiveSession_createsNewSession() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, CardType.PUBLIC, true);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.empty());
            given(studyDomainService.createSession(testUser)).willReturn(testSession);
            given(studyDomainService.processAnswer(eq(testUser), eq(testCard), eq(testSession), eq(true)))
                    .willReturn(testRecord);

            // when
            StudyResultResponse result = studyService.submitAnswer(testUser, request);

            // then
            verify(studyDomainService).createSession(testUser);
            assertThat(result.cardId()).isEqualTo(CARD_ID);
        }

        @Test
        @DisplayName("오답을 제출하고 결과를 반환한다")
        void submitAnswer_withIncorrectAnswer_returnsResult() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, CardType.PUBLIC, false);
            StudyRecord incorrectRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .session(testSession)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now().plusDays(1))
                    .interval(1)
                    .efFactor(2.3)
                    .build();
            ReflectionTestUtils.setField(incorrectRecord, "id", 2L);

            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));
            given(studyDomainService.processAnswer(eq(testUser), eq(testCard), eq(testSession), eq(false)))
                    .willReturn(incorrectRecord);

            // when
            StudyResultResponse result = studyService.submitAnswer(testUser, request);

            // then
            assertThat(result.cardId()).isEqualTo(CARD_ID);
            assertThat(result.isCorrect()).isFalse();
        }

        @Test
        @DisplayName("답변 제출 시 사용자 스트릭을 업데이트한다")
        void submitAnswer_updatesUserStreak() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, CardType.PUBLIC, true);
            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));
            given(studyDomainService.processAnswer(any(), any(), any(), any())).willReturn(testRecord);

            // when
            studyService.submitAnswer(testUser, request);

            // then
            verify(cardDomainService).findById(CARD_ID);
            verify(studyDomainService).processAnswer(eq(testUser), eq(testCard), eq(testSession), eq(true));
        }

    }

    @Nested
    @DisplayName("endCurrentSession")
    class EndCurrentSessionTest {

        @Test
        @DisplayName("현재 활성 세션을 종료한다")
        void endCurrentSession_success() {
            // given
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));

            // when
            SessionResponse result = studyService.endCurrentSession(testUser);

            // then
            assertThat(result.id()).isEqualTo(SESSION_ID);
            assertThat(result.endedAt()).isNotNull();
        }

        @Test
        @DisplayName("활성 세션이 없으면 예외를 던진다")
        void endCurrentSession_noActiveSession_throwsException() {
            // given
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyService.endCurrentSession(testUser))
                    .isInstanceOf(StudyException.class)
                    .extracting(e -> ((StudyException) e).getErrorCode())
                    .isEqualTo(StudyErrorCode.NO_ACTIVE_SESSION);
        }
    }

    @Nested
    @DisplayName("getCurrentSession")
    class GetCurrentSessionTest {

        @Test
        @DisplayName("현재 활성 세션을 조회한다")
        void getCurrentSession_success() {
            // given
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.of(testSession));

            // when
            SessionResponse result = studyService.getCurrentSession(testUser);

            // then
            assertThat(result.id()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("활성 세션이 없으면 예외를 던진다")
        void getCurrentSession_noActiveSession_throwsException() {
            // given
            given(studyDomainService.findActiveSession(testUser)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyService.getCurrentSession(testUser))
                    .isInstanceOf(StudyException.class)
                    .extracting(e -> ((StudyException) e).getErrorCode())
                    .isEqualTo(StudyErrorCode.NO_ACTIVE_SESSION);
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSessionTest {

        @Test
        @DisplayName("특정 세션을 조회한다")
        void getSession_success() {
            // given
            given(studyDomainService.findSessionById(SESSION_ID)).willReturn(testSession);
            doNothing().when(studyDomainService).validateSessionOwnership(testSession, testUser);

            // when
            SessionResponse result = studyService.getSession(testUser, SESSION_ID);

            // then
            assertThat(result.id()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("다른 사용자의 세션 접근 시 예외를 던진다")
        void getSession_accessDenied_throwsException() {
            // given
            given(studyDomainService.findSessionById(SESSION_ID)).willReturn(testSession);
            doThrow(new StudyException(StudyErrorCode.SESSION_ACCESS_DENIED))
                    .when(studyDomainService).validateSessionOwnership(testSession, testUser);

            // when & then
            assertThatThrownBy(() -> studyService.getSession(testUser, SESSION_ID))
                    .isInstanceOf(StudyException.class)
                    .extracting(e -> ((StudyException) e).getErrorCode())
                    .isEqualTo(StudyErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("getSessionHistory")
    class GetSessionHistoryTest {

        @Test
        @DisplayName("세션 히스토리를 페이지네이션하여 조회한다")
        void getSessionHistory_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startedAt"));
            Page<StudySession> sessions = new PageImpl<>(List.of(testSession), pageable, 1);
            given(studyDomainService.findSessionHistory(testUser, pageable)).willReturn(sessions);

            // when
            Page<SessionResponse> result = studyService.getSessionHistory(testUser, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(SESSION_ID);
        }
    }

    @Nested
    @DisplayName("getSessionStats")
    class GetSessionStatsTest {

        @Test
        @DisplayName("세션 상세 통계를 조회한다")
        void getSessionStats_success() {
            // given
            given(studyDomainService.findSessionById(SESSION_ID)).willReturn(testSession);
            doNothing().when(studyDomainService).validateSessionOwnership(testSession, testUser);
            given(studyDomainService.findRecordsBySession(testSession)).willReturn(List.of(testRecord));

            // when
            SessionStatsResponse result = studyService.getSessionStats(testUser, SESSION_ID);

            // then
            assertThat(result.id()).isEqualTo(SESSION_ID);
            assertThat(result.records()).hasSize(1);
        }
    }
}
