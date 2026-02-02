package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StudySessionTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .roles(Set.of(Role.ROLE_USER))
                .build();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("학습 세션을 생성한다")
        void builder_createsSession() {
            // when
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // then
            assertThat(session.getUser()).isEqualTo(testUser);
            assertThat(session.getStartedAt()).isNotNull();
            assertThat(session.getEndedAt()).isNull();
        }

        @Test
        @DisplayName("totalCards 기본값은 0이다")
        void builder_totalCardsDefaultsToZero() {
            // when
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // then
            assertThat(session.getTotalCards()).isEqualTo(0);
        }

        @Test
        @DisplayName("correctCount 기본값은 0이다")
        void builder_correctCountDefaultsToZero() {
            // when
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // then
            assertThat(session.getCorrectCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("endSession")
    class EndSessionTest {

        @Test
        @DisplayName("세션 종료 시 endedAt이 설정된다")
        void endSession_setsEndedAt() {
            // given
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // when
            session.endSession();

            // then
            assertThat(session.getEndedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("incrementTotalCards")
    class IncrementTotalCardsTest {

        @Test
        @DisplayName("totalCards를 1 증가시킨다")
        void incrementTotalCards_increasesByOne() {
            // given
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();
            int initialCount = session.getTotalCards();

            // when
            session.incrementTotalCards();

            // then
            assertThat(session.getTotalCards()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("여러 번 호출하면 누적된다")
        void incrementTotalCards_accumulates() {
            // given
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // when
            session.incrementTotalCards();
            session.incrementTotalCards();
            session.incrementTotalCards();

            // then
            assertThat(session.getTotalCards()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("incrementCorrectCount")
    class IncrementCorrectCountTest {

        @Test
        @DisplayName("correctCount를 1 증가시킨다")
        void incrementCorrectCount_increasesByOne() {
            // given
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();
            int initialCount = session.getCorrectCount();

            // when
            session.incrementCorrectCount();

            // then
            assertThat(session.getCorrectCount()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("여러 번 호출하면 누적된다")
        void incrementCorrectCount_accumulates() {
            // given
            StudySession session = StudySession.builder()
                    .user(testUser)
                    .build();

            // when
            session.incrementCorrectCount();
            session.incrementCorrectCount();

            // then
            assertThat(session.getCorrectCount()).isEqualTo(2);
        }
    }
}
