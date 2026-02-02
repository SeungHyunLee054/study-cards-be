package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.service.StudyDomainService;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class StudyServiceUnitTest extends BaseUnitTest {

    @Mock
    private StudyDomainService studyDomainService;

    @Mock
    private CardDomainService cardDomainService;

    @InjectMocks
    private StudyService studyService;

    private User testUser;
    private Card testCard;

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testCard = createTestCard();
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
    @DisplayName("getTodayCards")
    class GetTodayCardsTest {

        @Test
        @DisplayName("오늘 학습할 카드 목록을 반환한다")
        void getTodayCards_returnsCardList() {
            // given
            given(studyDomainService.findTodayStudyCards(testUser, Category.CS))
                    .willReturn(List.of(testCard));

            // when
            List<StudyCardResponse> result = studyService.getTodayCards(testUser, Category.CS);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CARD_ID);
            assertThat(result.get(0).questionEn()).isEqualTo("What is Java?");
            assertThat(result.get(0).questionKo()).isEqualTo("자바란 무엇인가?");
            assertThat(result.get(0).category()).isEqualTo(Category.CS);
        }

        @Test
        @DisplayName("StudyDomainService를 호출한다")
        void getTodayCards_callsStudyDomainService() {
            // given
            given(studyDomainService.findTodayStudyCards(testUser, Category.CS))
                    .willReturn(List.of(testCard));

            // when
            studyService.getTodayCards(testUser, Category.CS);

            // then
            verify(studyDomainService).findTodayStudyCards(testUser, Category.CS);
        }
    }

    @Nested
    @DisplayName("submitAnswer")
    class SubmitAnswerTest {

        @Test
        @DisplayName("정답 제출 시 StudyResultResponse를 반환한다")
        void submitAnswer_returnsStudyResultResponse() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, true);
            LocalDate nextReviewDate = LocalDate.now().plusDays(1);

            StudyRecord studyRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(true)
                    .nextReviewDate(nextReviewDate)
                    .interval(1)
                    .efFactor(2.5)
                    .build();

            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.processAnswer(eq(testUser), eq(testCard), any(), eq(true)))
                    .willReturn(studyRecord);

            // when
            StudyResultResponse result = studyService.submitAnswer(testUser, request);

            // then
            assertThat(result.cardId()).isEqualTo(CARD_ID);
            assertThat(result.isCorrect()).isTrue();
            assertThat(result.nextReviewDate()).isEqualTo(nextReviewDate);
            assertThat(result.newEfFactor()).isEqualTo(studyRecord.getEfFactor());
        }

        @Test
        @DisplayName("CardDomainService와 StudyDomainService를 호출한다")
        void submitAnswer_callsDomainServices() {
            // given
            StudyAnswerRequest request = new StudyAnswerRequest(CARD_ID, false);
            LocalDate nextReviewDate = LocalDate.now().plusDays(1);

            StudyRecord studyRecord = StudyRecord.builder()
                    .user(testUser)
                    .card(testCard)
                    .isCorrect(false)
                    .nextReviewDate(nextReviewDate)
                    .interval(1)
                    .efFactor(2.18)
                    .build();

            given(cardDomainService.findById(CARD_ID)).willReturn(testCard);
            given(studyDomainService.processAnswer(eq(testUser), eq(testCard), any(), eq(false)))
                    .willReturn(studyRecord);

            // when
            studyService.submitAnswer(testUser, request);

            // then
            verify(cardDomainService).findById(CARD_ID);
            verify(studyDomainService).processAnswer(eq(testUser), eq(testCard), any(), eq(false));
        }
    }
}
