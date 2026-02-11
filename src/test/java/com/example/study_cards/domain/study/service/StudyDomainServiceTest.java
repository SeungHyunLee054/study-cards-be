package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.repository.CardRepository;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import com.example.study_cards.support.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class StudyDomainServiceTest extends BaseUnitTest {

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private StudyRecordRepository studyRecordRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserCardRepository userCardRepository;

    @InjectMocks
    private StudyDomainService studyDomainService;

    private User testUser;
    private Card testCard;
    private Category testCategory;
    private StudySession testSession;
    private StudyRecord testRecord;
    private UserCard testUserCard;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;
    private static final Long SESSION_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testCategory = createTestCategory();
        testCard = createTestCard();
        testSession = createTestSession();
        testUserCard = createTestUserCard();
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
                .answer("테스트 답변")
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
                .answer("사용자 답변")
                .efFactor(2.5)
                .category(testCategory)
                .build();
        ReflectionTestUtils.setField(userCard, "id", 1L);
        return userCard;
    }

    @Nested
    @DisplayName("createSession")
    class CreateSessionTest {

        @Test
        @DisplayName("새 학습 세션을 생성한다")
        void createSession_createsNewSession() {
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
        @DisplayName("존재하는 ID로 세션을 조회한다")
        void findSessionById_returnsSession() {
            // given
            given(studySessionRepository.findById(SESSION_ID)).willReturn(Optional.of(testSession));

            // when
            StudySession result = studyDomainService.findSessionById(SESSION_ID);

            // then
            assertThat(result.getId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
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
    @DisplayName("findTodayStudyCards")
    class FindTodayStudyCardsTest {

        @Test
        @DisplayName("복습할 카드가 충분하면 복습 카드만 반환한다")
        void findTodayStudyCards_withEnoughDueCards_returnsDueCardsOnly() {
            // given
            StudyRecord dueRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .build();
            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), any()))
                    .willReturn(List.of(dueRecord));

            // when
            List<Card> result = studyDomainService.findTodayStudyCards(testUser, testCategory, 1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCard);
        }

        @Test
        @DisplayName("복습할 카드가 부족하면 새 카드를 추가한다")
        void findTodayStudyCards_withNotEnoughDueCards_addsNewCards() {
            // given
            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), any()))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findStudiedCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(cardRepository.findByCategoryOrderByEfFactorAsc(eq(testCategory), anyBoolean()))
                    .willReturn(List.of(testCard));

            // when
            List<Card> result = studyDomainService.findTodayStudyCards(testUser, testCategory, 20);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCard);
        }
    }

    @Nested
    @DisplayName("processAnswer")
    class ProcessAnswerTest {

        @Test
        @DisplayName("새 카드에 대한 답변을 처리한다")
        void processAnswer_withNewCard_createsNewRecord() {
            // given
            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.empty());
            given(studyRecordRepository.save(any(StudyRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            StudyRecord result = studyDomainService.processAnswer(testUser, testCard, testSession, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getIsCorrect()).isTrue();
            verify(studyRecordRepository).save(any(StudyRecord.class));
        }

        @Test
        @DisplayName("기존 카드에 대한 답변을 처리한다")
        void processAnswer_withExistingCard_updatesRecord() {
            // given
            testRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .interval(1)
                    .efFactor(2.5)
                    .build();
            ReflectionTestUtils.setField(testRecord, "id", 1L);
            ReflectionTestUtils.setField(testRecord, "repetitionCount", 1);

            given(studyRecordRepository.findByUserAndCard(testUser, testCard))
                    .willReturn(Optional.of(testRecord));

            // when
            StudyRecord result = studyDomainService.processAnswer(testUser, testCard, testSession, true);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("calculateInterval")
    class CalculateIntervalTest {

        @Test
        @DisplayName("오답일 경우 간격은 1일이다")
        void calculateInterval_withIncorrect_returnsOne() {
            // when
            int interval = studyDomainService.calculateInterval(1, 2.5, false);

            // then
            assertThat(interval).isEqualTo(1);
        }

        @Test
        @DisplayName("첫 번째 정답일 경우 간격은 1일이다")
        void calculateInterval_withFirstCorrect_returnsOne() {
            // when
            int interval = studyDomainService.calculateInterval(1, 2.5, true);

            // then
            assertThat(interval).isEqualTo(1);
        }

        @Test
        @DisplayName("두 번째 정답일 경우 간격은 6일이다")
        void calculateInterval_withSecondCorrect_returnsSix() {
            // when
            int interval = studyDomainService.calculateInterval(2, 2.5, true);

            // then
            assertThat(interval).isEqualTo(6);
        }

        @Test
        @DisplayName("세 번째 이후 정답일 경우 간격을 계산한다")
        void calculateInterval_withThirdOrMoreCorrect_calculatesInterval() {
            // when
            int interval = studyDomainService.calculateInterval(3, 2.5, true);

            // then
            assertThat(interval).isGreaterThan(6);
        }
    }

    @Nested
    @DisplayName("findTodayUserCardsForStudy")
    class FindTodayUserCardsForStudyTest {

        @Test
        @DisplayName("사용자 카드를 조회한다")
        void findTodayUserCardsForStudy_returnsUserCards() {
            // given
            given(studyRecordRepository.findDueUserCardRecordsByCategory(any(), any(), any()))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findStudiedUserCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(userCardRepository.findByUserAndCategoryOrderByEfFactorAsc(testUser, testCategory))
                    .willReturn(List.of(testUserCard));

            // when
            List<UserCard> result = studyDomainService.findTodayUserCardsForStudy(testUser, testCategory, 20);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testUserCard);
        }

        @Test
        @DisplayName("카테고리 없이 모든 사용자 카드를 조회한다")
        void findTodayUserCardsForStudy_withoutCategory_returnsAllUserCards() {
            // given
            given(studyRecordRepository.findDueUserCardRecords(any(), any()))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findStudiedUserCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(userCardRepository.findByUserOrderByEfFactorAsc(testUser))
                    .willReturn(List.of(testUserCard));

            // when
            List<UserCard> result = studyDomainService.findTodayUserCardsForStudy(testUser, null, 20);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("processUserCardAnswer")
    class ProcessUserCardAnswerTest {

        @Test
        @DisplayName("새 사용자 카드에 대한 답변을 처리한다")
        void processUserCardAnswer_withNewCard_createsNewRecord() {
            // given
            given(studyRecordRepository.findByUserAndUserCard(testUser, testUserCard))
                    .willReturn(Optional.empty());
            given(studyRecordRepository.save(any(StudyRecord.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            StudyRecord result = studyDomainService.processUserCardAnswer(testUser, testUserCard, testSession, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getIsCorrect()).isTrue();
            verify(studyRecordRepository).save(any(StudyRecord.class));
        }
    }

}
