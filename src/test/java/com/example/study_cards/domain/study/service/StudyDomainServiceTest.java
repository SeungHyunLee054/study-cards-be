package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class StudyDomainServiceTest extends BaseUnitTest {

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private StudyDomainService studyDomainService;

    private User testUser;
    private Card testCard;
    private StudySession testSession;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;
    private static final Long SESSION_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testCard = createTestCard();
        testSession = createTestSession();
    }

    private User createTestUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .roles(Set.of(Role.ROLE_USER))
                .build();
        setId(user, User.class, USER_ID);
        return user;
    }

    private Card createTestCard() {
        Card card = Card.builder()
                .questionEn("What is Java?")
                .questionKo("자바란 무엇인가?")
                .answerEn("A programming language")
                .answerKo("프로그래밍 언어")
                .efFactor(2.5)
                .category(Category.CS)
                .build();
        setId(card, Card.class, CARD_ID);
        return card;
    }

    private StudySession createTestSession() {
        StudySession session = StudySession.builder()
                .user(testUser)
                .build();
        setId(session, StudySession.class, SESSION_ID);
        return session;
    }

    private <T> void setId(T entity, Class<T> clazz, Long id) {
        try {
            var idField = clazz.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("createSession")
    class CreateSessionTest {

        @Test
        @DisplayName("학습 세션을 생성하고 저장한다")
        void createSession_savesAndReturnsSession() {
            // given
            given(studySessionRepository.save(any(StudySession.class))).willReturn(testSession);

            // when
            StudySession result = studyDomainService.createSession(testUser);

            // then
            assertThat(result).isNotNull();
            verify(studySessionRepository).save(any(StudySession.class));
        }
    }

    @Nested
    @DisplayName("findSessionById")
    class FindSessionByIdTest {

        @Test
        @DisplayName("존재하는 세션 ID로 세션을 조회한다")
        void findSessionById_returnsSession() {
            // given
            given(studySessionRepository.findById(SESSION_ID)).willReturn(Optional.of(testSession));

            // when
            StudySession result = studyDomainService.findSessionById(SESSION_ID);

            // then
            assertThat(result.getId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID로 조회 시 예외를 발생시킨다")
        void findSessionById_withNonExistentId_throwsException() {
            // given
            given(studySessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyDomainService.findSessionById(SESSION_ID))
                    .isInstanceOf(StudyException.class)
                    .satisfies(exception -> {
                        StudyException studyException = (StudyException) exception;
                        assertThat(studyException.getErrorCode()).isEqualTo(StudyErrorCode.SESSION_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("calculateInterval")
    class CalculateIntervalTest {

        @Test
        @DisplayName("오답 시 interval은 1이다")
        void calculateInterval_incorrect_returns1() {
            // when
            int interval = studyDomainService.calculateInterval(1, 2.5, false);

            // then
            assertThat(interval).isEqualTo(1);
        }

        @Test
        @DisplayName("첫 번째 정답 시 interval은 1이다")
        void calculateInterval_firstCorrect_returns1() {
            // when
            int interval = studyDomainService.calculateInterval(1, 2.5, true);

            // then
            assertThat(interval).isEqualTo(1);
        }

        @Test
        @DisplayName("두 번째 정답 시 interval은 6이다")
        void calculateInterval_secondCorrect_returns6() {
            // when
            int interval = studyDomainService.calculateInterval(2, 2.5, true);

            // then
            assertThat(interval).isEqualTo(6);
        }

        @Test
        @DisplayName("세 번째 정답 시 interval은 prevInterval * efFactor이다")
        void calculateInterval_thirdCorrect_returnsCalculatedValue() {
            // when
            int interval = studyDomainService.calculateInterval(3, 2.5, true);

            // then
            assertThat(interval).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("findTodayStudyCards")
    class FindTodayStudyCardsTest {

        @Test
        @DisplayName("복습 대상 카드와 새 카드를 합쳐서 반환한다")
        void findTodayStudyCards_returnsCombinedCards() {
            // given
            StudyRecord dueRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            Card newCard = Card.builder()
                    .questionEn("New card")
                    .questionKo("새 카드")
                    .answerEn("Answer")
                    .answerKo("답변")
                    .efFactor(2.5)
                    .category(Category.CS)
                    .build();
            setId(newCard, Card.class, 2L);

            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), any()))
                    .willReturn(List.of(dueRecord));
            given(studyRecordRepository.findStudiedCardIdsByUser(testUser))
                    .willReturn(List.of(CARD_ID));
            given(cardRepository.findByCategoryOrderByEfFactorAsc(Category.CS))
                    .willReturn(List.of(testCard, newCard));

            // when
            List<Card> result = studyDomainService.findTodayStudyCards(testUser, Category.CS, 20);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).contains(testCard);
            assertThat(result).contains(newCard);
        }

        @Test
        @DisplayName("복습 대상이 limit을 넘으면 새 카드는 포함하지 않는다")
        void findTodayStudyCards_whenDueCardsExceedLimit_returnsOnlyDueCards() {
            // given
            StudyRecord dueRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), any()))
                    .willReturn(List.of(dueRecord));

            // when
            List<Card> result = studyDomainService.findTodayStudyCards(testUser, Category.CS, 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCard);
        }

        @Test
        @DisplayName("복습 대상이 없으면 새 카드만 반환한다")
        void findTodayStudyCards_whenNoDueCards_returnsNewCardsOnly() {
            // given
            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), any()))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findStudiedCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(cardRepository.findByCategoryOrderByEfFactorAsc(Category.CS))
                    .willReturn(List.of(testCard));

            // when
            List<Card> result = studyDomainService.findTodayStudyCards(testUser, Category.CS, 20);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCard);
        }
    }

    @Nested
    @DisplayName("processAnswer")
    class ProcessAnswerTest {

        @Test
        @DisplayName("새 카드에 대한 정답 처리 시 StudyRecord를 생성한다")
        void processAnswer_newCard_createsRecord() {
            // given
            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.empty());
            given(studyRecordRepository.save(any(StudyRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            StudyRecord result = studyDomainService.processAnswer(testUser, testCard, null, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getIsCorrect()).isTrue();
            assertThat(result.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
            assertThat(result.getInterval()).isEqualTo(1);
            verify(studyRecordRepository).save(any(StudyRecord.class));
        }

        @Test
        @DisplayName("기존 카드에 대한 정답 처리 시 StudyRecord를 업데이트한다")
        void processAnswer_existingCard_updatesRecord() {
            // given
            StudyRecord existingRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.of(existingRecord));

            // when
            StudyRecord result = studyDomainService.processAnswer(testUser, testCard, null, true);

            // then
            assertThat(result.getRepetitionCount()).isEqualTo(2);
            assertThat(result.getIsCorrect()).isTrue();
        }

        @Test
        @DisplayName("오답 처리 시 nextReviewDate는 내일로 설정된다")
        void processAnswer_incorrect_setsNextReviewDateToTomorrow() {
            // given
            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.empty());
            given(studyRecordRepository.save(any(StudyRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            StudyRecord result = studyDomainService.processAnswer(testUser, testCard, null, false);

            // then
            assertThat(result.getNextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
            assertThat(result.getIsCorrect()).isFalse();
        }

        @Test
        @DisplayName("세션과 함께 처리 시 세션 카운트가 증가한다")
        void processAnswer_withSession_incrementsSessionCount() {
            // given
            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.empty());
            given(studyRecordRepository.save(any(StudyRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            int initialTotalCards = testSession.getTotalCards();
            int initialCorrectCount = testSession.getCorrectCount();

            // when
            studyDomainService.processAnswer(testUser, testCard, testSession, true);

            // then
            assertThat(testSession.getTotalCards()).isEqualTo(initialTotalCards + 1);
            assertThat(testSession.getCorrectCount()).isEqualTo(initialCorrectCount + 1);
        }
    }
}
