package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.CardStatus;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.*;
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
    @DisplayName("findTodayAllStudyCards")
    class FindTodayAllStudyCardsTest {

        @Test
        @DisplayName("UserCard 복습 카드가 공용 Card 복습 카드보다 먼저 반환된다")
        void findTodayAllStudyCards_userCardDueFirst() {
            // given
            StudyRecord cardDueRecord = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(2.5).build();
            StudyRecord userCardDueRecord = StudyRecord.builder()
                    .user(testUser).userCard(testUserCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(2.5).build();

            given(studyRecordRepository.findDueUserCardRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(List.of(userCardDueRecord));
            given(studyRecordRepository.findDueRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(List.of(cardDueRecord));

            // when
            var result = studyDomainService.findTodayAllStudyCards(testUser, testCategory, 20);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).isUserCard()).isTrue();
            assertThat(result.get(1).isPublicCard()).isTrue();
        }

        @Test
        @DisplayName("복습 카드가 부족하면 미학습 UserCard를 먼저 보충한다")
        void findTodayAllStudyCards_newUserCardsBeforeNewPublicCards() {
            // given
            given(studyRecordRepository.findDueUserCardRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findDueRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(Collections.emptyList());
            given(studyRecordRepository.findStudiedUserCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(userCardRepository.findByUserAndCategoriesOrderByEfFactorAsc(testUser, List.of(testCategory)))
                    .willReturn(List.of(testUserCard));
            given(studyRecordRepository.findStudiedCardIdsByUser(testUser))
                    .willReturn(Collections.emptyList());
            given(cardRepository.findByCategoriesOrderByEfFactorAsc(List.of(testCategory)))
                    .willReturn(List.of(testCard));

            // when
            var result = studyDomainService.findTodayAllStudyCards(testUser, testCategory, 20);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).isUserCard()).isTrue();
            assertThat(result.get(1).isPublicCard()).isTrue();
        }

        @Test
        @DisplayName("limit 이상의 카드를 반환하지 않는다")
        void findTodayAllStudyCards_respectsLimit() {
            // given
            StudyRecord dueRecord = StudyRecord.builder()
                    .user(testUser).userCard(testUserCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(2.5).build();

            given(studyRecordRepository.findDueUserCardRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(List.of(dueRecord));
            given(studyRecordRepository.findDueRecordsByCategories(eq(testUser), any(), eq(List.of(testCategory))))
                    .willReturn(Collections.emptyList());

            // when
            var result = studyDomainService.findTodayAllStudyCards(testUser, testCategory, 1);

            // then
            assertThat(result).hasSize(1);
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

    @Nested
    @DisplayName("calculatePriorityScore")
    class CalculatePriorityScoreTest {

        @Test
        @DisplayName("반복 오답 카드는 1000점 추가")
        void calculatePriorityScore_repeatedMistake_adds1000() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(2.5)
                    .build();

            List<Long> repeatedMistakeIds = List.of(CARD_ID);
            List<Long> overdueIds = List.of();
            List<Long> recentWrongIds = List.of();

            // when
            int score = studyDomainService.calculatePriorityScore(record, repeatedMistakeIds, overdueIds, recentWrongIds);

            // then
            assertThat(score).isGreaterThanOrEqualTo(1000);
        }

        @Test
        @DisplayName("미복습 카드는 500점 추가")
        void calculatePriorityScore_overdue_adds500() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(2.5)
                    .build();

            List<Long> repeatedMistakeIds = List.of();
            List<Long> overdueIds = List.of(CARD_ID);
            List<Long> recentWrongIds = List.of();

            // when
            int score = studyDomainService.calculatePriorityScore(record, repeatedMistakeIds, overdueIds, recentWrongIds);

            // then
            assertThat(score).isGreaterThanOrEqualTo(500);
        }

        @Test
        @DisplayName("최근 오답 카드는 300점 추가")
        void calculatePriorityScore_recentWrong_adds300() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(false)
                    .nextReviewDate(LocalDate.now())
                    .efFactor(2.5)
                    .build();

            List<Long> repeatedMistakeIds = List.of();
            List<Long> overdueIds = List.of();
            List<Long> recentWrongIds = List.of(CARD_ID);

            // when
            int score = studyDomainService.calculatePriorityScore(record, repeatedMistakeIds, overdueIds, recentWrongIds);

            // then
            assertThat(score).isGreaterThanOrEqualTo(300);
        }

        @Test
        @DisplayName("낮은 efFactor는 높은 점수")
        void calculatePriorityScore_lowEfFactor_higherScore() {
            // given
            StudyRecord lowEf = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(1.3).build();

            StudyRecord highEf = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(2.5).build();

            List<Long> empty = List.of();

            // when
            int lowEfScore = studyDomainService.calculatePriorityScore(lowEf, empty, empty, empty);
            int highEfScore = studyDomainService.calculatePriorityScore(highEf, empty, empty, empty);

            // then
            assertThat(lowEfScore).isGreaterThan(highEfScore);
        }

        @Test
        @DisplayName("모든 조건을 만족하면 점수가 합산된다")
        void calculatePriorityScore_allConditions_scoresAccumulate() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(false)
                    .nextReviewDate(LocalDate.now()).efFactor(1.3).build();

            List<Long> ids = List.of(CARD_ID);

            // when
            int score = studyDomainService.calculatePriorityScore(record, ids, ids, ids);

            // then
            assertThat(score).isGreaterThanOrEqualTo(1800 + 0); // 1000 + 500 + 300 + efFactor score
        }
    }

    @Nested
    @DisplayName("findPrioritizedDueRecords")
    class FindPrioritizedDueRecordsTest {

        @Test
        @DisplayName("우선순위 점수 기반으로 정렬된 복습 카드를 반환한다")
        void findPrioritizedDueRecords_returnsSortedByScore() {
            // given
            Card card2 = Card.builder()
                    .question("질문2").answer("답변2")
                    .efFactor(2.5).category(testCategory).build();
            ReflectionTestUtils.setField(card2, "id", 2L);

            StudyRecord record1 = StudyRecord.builder()
                    .user(testUser).card(testCard).isCorrect(true)
                    .nextReviewDate(LocalDate.now()).efFactor(2.5).build();

            StudyRecord record2 = StudyRecord.builder()
                    .user(testUser).card(card2).isCorrect(false)
                    .nextReviewDate(LocalDate.now()).efFactor(1.3).build();

            given(studyRecordRepository.findDueRecordsByCategory(any(), any(), eq(null)))
                    .willReturn(List.of(record1, record2));
            given(studyRecordRepository.findDueUserCardRecords(any(), any()))
                    .willReturn(List.of());
            given(studyRecordRepository.findRepeatedMistakeRecords(any(), eq(3)))
                    .willReturn(List.of());
            given(studyRecordRepository.findOverdueRecords(any(), any(), eq(7)))
                    .willReturn(List.of());
            given(studyRecordRepository.findRecentWrongRecords(any(), eq(20)))
                    .willReturn(List.of(record2));

            // when
            var result = studyDomainService.findPrioritizedDueRecords(testUser, 20);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).score()).isGreaterThanOrEqualTo(result.get(1).score());
        }
    }

    @Nested
    @DisplayName("endSession")
    class EndSessionTest {

        @Test
        @DisplayName("세션을 종료한다")
        void endSession_success() {
            // given
            given(studySessionRepository.findById(SESSION_ID)).willReturn(Optional.of(testSession));

            // when
            studyDomainService.endSession(SESSION_ID);

            // then
            assertThat(testSession.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 세션 종료 시 예외를 발생시킨다")
        void endSession_notFound_throwsException() {
            // given
            given(studySessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> studyDomainService.endSession(SESSION_ID))
                    .isInstanceOf(StudyException.class)
                    .satisfies(exception -> {
                        StudyException e = (StudyException) exception;
                        assertThat(e.getErrorCode()).isEqualTo(StudyErrorCode.SESSION_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("findActiveSession")
    class FindActiveSessionTest {

        @Test
        @DisplayName("활성 세션이 있으면 반환한다")
        void findActiveSession_exists_returnsSession() {
            // given
            given(studySessionRepository.findByUserAndEndedAtIsNull(testUser))
                    .willReturn(Optional.of(testSession));

            // when
            Optional<StudySession> result = studyDomainService.findActiveSession(testUser);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("활성 세션이 없으면 빈 Optional을 반환한다")
        void findActiveSession_notExists_returnsEmpty() {
            // given
            given(studySessionRepository.findByUserAndEndedAtIsNull(testUser))
                    .willReturn(Optional.empty());

            // when
            Optional<StudySession> result = studyDomainService.findActiveSession(testUser);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findSessionHistory")
    class FindSessionHistoryTest {

        @Test
        @DisplayName("사용자의 세션 히스토리를 페이지네이션으로 조회한다")
        void findSessionHistory_returnsPagedResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<StudySession> page = new PageImpl<>(List.of(testSession), pageable, 1);
            given(studySessionRepository.findByUserOrderByStartedAtDesc(testUser, pageable))
                    .willReturn(page);

            // when
            Page<StudySession> result = studyDomainService.findSessionHistory(testUser, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("validateSessionOwnership")
    class ValidateSessionOwnershipTest {

        @Test
        @DisplayName("세션 소유자와 요청 유저가 같으면 정상 통과한다")
        void validateSessionOwnership_sameUser_success() {
            // when & then (예외 없이 통과)
            studyDomainService.validateSessionOwnership(testSession, testUser);
        }

        @Test
        @DisplayName("세션 소유자와 요청 유저가 다르면 예외를 발생시킨다")
        void validateSessionOwnership_differentUser_throwsException() {
            // given
            User otherUser = User.builder()
                    .email("other@example.com")
                    .password("password")
                    .nickname("다른유저")
                    .build();
            ReflectionTestUtils.setField(otherUser, "id", 999L);

            // when & then
            assertThatThrownBy(() -> studyDomainService.validateSessionOwnership(testSession, otherUser))
                    .isInstanceOf(StudyException.class)
                    .satisfies(exception -> {
                        StudyException e = (StudyException) exception;
                        assertThat(e.getErrorCode()).isEqualTo(StudyErrorCode.SESSION_ACCESS_DENIED);
                    });
        }
    }

    @Nested
    @DisplayName("countDueCards")
    class CountDueCardsTest {

        @Test
        @DisplayName("복습 대상 카드 수를 반환한다")
        void countDueCards_returnsCount() {
            // given
            given(studyRecordRepository.countDueCards(testUser, LocalDate.now())).willReturn(5);

            // when
            int result = studyDomainService.countDueCards(testUser, LocalDate.now());

            // then
            assertThat(result).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("countTotalStudiedCards")
    class CountTotalStudiedCardsTest {

        @Test
        @DisplayName("총 학습한 카드 수를 반환한다")
        void countTotalStudiedCards_returnsCount() {
            // given
            given(studyRecordRepository.countTotalStudiedCards(testUser)).willReturn(42);

            // when
            int result = studyDomainService.countTotalStudiedCards(testUser);

            // then
            assertThat(result).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("countTotalAndCorrect")
    class CountTotalAndCorrectTest {

        @Test
        @DisplayName("전체 학습 수와 정답 수를 반환한다")
        void countTotalAndCorrect_returnsCounts() {
            // given
            TotalAndCorrect expected = new TotalAndCorrect(100L, 80L);
            given(studyRecordRepository.countTotalAndCorrect(testUser)).willReturn(expected);

            // when
            TotalAndCorrect result = studyDomainService.countTotalAndCorrect(testUser);

            // then
            assertThat(result.totalCount()).isEqualTo(100L);
            assertThat(result.correctCount()).isEqualTo(80L);
        }
    }

    @Nested
    @DisplayName("countTodayStudy")
    class CountTodayStudyTest {

        @Test
        @DisplayName("오늘 학습 수와 정답 수를 반환한다")
        void countTodayStudy_returnsCounts() {
            // given
            var todayCount = new TodayStudyCount(10L, 7L);
            given(studyRecordRepository.countTodayStudy(testUser, LocalDate.now())).willReturn(todayCount);

            // when
            TotalAndCorrect result = studyDomainService.countTodayStudy(testUser, LocalDate.now());

            // then
            assertThat(result.totalCount()).isEqualTo(10L);
            assertThat(result.correctCount()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("countStudiedByCategory")
    class CountStudiedByCategoryTest {

        @Test
        @DisplayName("카테고리별 학습 카드 수를 반환한다")
        void countStudiedByCategory_returnsCategoryCounts() {
            // given
            List<CategoryCount> expected = List.of(new CategoryCount(CATEGORY_ID, "CS", 15L));
            given(studyRecordRepository.countStudiedByCategory(testUser)).willReturn(expected);

            // when
            List<CategoryCount> result = studyDomainService.countStudiedByCategory(testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryCode()).isEqualTo("CS");
            assertThat(result.get(0).count()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("countLearningByCategory")
    class CountLearningByCategoryTest {

        @Test
        @DisplayName("카테고리별 학습 중인 카드 수를 반환한다")
        void countLearningByCategory_returnsCategoryCounts() {
            // given
            List<CategoryCount> expected = List.of(new CategoryCount(CATEGORY_ID, "CS", 8L));
            given(studyRecordRepository.countLearningByCategory(testUser)).willReturn(expected);

            // when
            List<CategoryCount> result = studyDomainService.countLearningByCategory(testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(8L);
        }
    }

    @Nested
    @DisplayName("countDueByCategory")
    class CountDueByCategoryTest {

        @Test
        @DisplayName("카테고리별 복습 대상 카드 수를 반환한다")
        void countDueByCategory_returnsCategoryCounts() {
            // given
            List<CategoryCount> expected = List.of(new CategoryCount(CATEGORY_ID, "CS", 3L));
            given(studyRecordRepository.countDueByCategory(testUser, LocalDate.now())).willReturn(expected);

            // when
            List<CategoryCount> result = studyDomainService.countDueByCategory(testUser, LocalDate.now());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("countMasteredByCategory")
    class CountMasteredByCategoryTest {

        @Test
        @DisplayName("카테고리별 마스터한 카드 수를 반환한다")
        void countMasteredByCategory_returnsCategoryCounts() {
            // given
            List<CategoryCount> expected = List.of(new CategoryCount(CATEGORY_ID, "CS", 20L));
            given(studyRecordRepository.countMasteredByCategory(testUser)).willReturn(expected);

            // when
            List<CategoryCount> result = studyDomainService.countMasteredByCategory(testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).count()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("findDailyActivity")
    class FindDailyActivityTest {

        @Test
        @DisplayName("일별 학습 활동 내역을 반환한다")
        void findDailyActivity_returnsActivities() {
            // given
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<DailyActivity> expected = List.of(
                    new DailyActivity(LocalDate.now(), 10L, 8L),
                    new DailyActivity(LocalDate.now().minusDays(1), 5L, 3L)
            );
            given(studyRecordRepository.findDailyActivity(testUser, since)).willReturn(expected);

            // when
            List<DailyActivity> result = studyDomainService.findDailyActivity(testUser, since);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).totalCount()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("findRecordsBySession")
    class FindRecordsBySessionTest {

        @Test
        @DisplayName("세션에 속한 학습 기록을 반환한다")
        void findRecordsBySession_returnsRecords() {
            // given
            StudyRecord record = StudyRecord.builder()
                    .user(testUser).card(testCard).session(testSession)
                    .isCorrect(true).nextReviewDate(LocalDate.now()).efFactor(2.5).build();
            given(studyRecordRepository.findBySessionWithDetails(testSession)).willReturn(List.of(record));

            // when
            List<StudyRecord> result = studyDomainService.findRecordsBySession(testSession);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("isCategoryFullyMastered")
    class IsCategoryFullyMasteredTest {

        @Test
        @DisplayName("카테고리에 카드가 없으면 false를 반환한다")
        void isCategoryFullyMastered_noCards_returnsFalse() {
            // given
            given(cardRepository.countByCategoryAndStatus(testCategory, CardStatus.ACTIVE)).willReturn(0L);

            // when
            boolean result = studyDomainService.isCategoryFullyMastered(testUser, testCategory);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("모든 카드를 마스터하면 true를 반환한다")
        void isCategoryFullyMastered_allMastered_returnsTrue() {
            // given
            given(cardRepository.countByCategoryAndStatus(testCategory, CardStatus.ACTIVE)).willReturn(10L);
            given(studyRecordRepository.countMasteredCardsInCategory(testUser, testCategory)).willReturn(10L);

            // when
            boolean result = studyDomainService.isCategoryFullyMastered(testUser, testCategory);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("일부만 마스터하면 false를 반환한다")
        void isCategoryFullyMastered_partial_returnsFalse() {
            // given
            given(cardRepository.countByCategoryAndStatus(testCategory, CardStatus.ACTIVE)).willReturn(10L);
            given(studyRecordRepository.countMasteredCardsInCategory(testUser, testCategory)).willReturn(5L);

            // when
            boolean result = studyDomainService.isCategoryFullyMastered(testUser, testCategory);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateCategoryAccuracy")
    class CalculateCategoryAccuracyTest {

        @Test
        @DisplayName("카테고리별 정확도를 반환한다")
        void calculateCategoryAccuracy_returnsAccuracies() {
            // given
            List<CategoryAccuracy> expected = List.of(
                    new CategoryAccuracy(CATEGORY_ID, "CS", "컴퓨터 과학", 50L, 40L, 0.8)
            );
            given(studyRecordRepository.calculateCategoryAccuracy(testUser)).willReturn(expected);

            // when
            List<CategoryAccuracy> result = studyDomainService.calculateCategoryAccuracy(testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).accuracy()).isEqualTo(0.8);
            assertThat(result.get(0).categoryName()).isEqualTo("컴퓨터 과학");
        }
    }
}
