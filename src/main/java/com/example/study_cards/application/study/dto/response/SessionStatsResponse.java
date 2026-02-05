package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.entity.StudySession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record SessionStatsResponse(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer totalCards,
        Integer correctCount,
        Double accuracyRate,
        Long durationSeconds,
        List<SessionRecordResponse> records
) {

    public static SessionStatsResponse from(StudySession session, List<StudyRecord> records) {
        double accuracy = session.getTotalCards() > 0
                ? Math.round((double) session.getCorrectCount() / session.getTotalCards() * 1000.0) / 10.0
                : 0.0;

        Long duration = null;
        if (session.getEndedAt() != null) {
            duration = Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds();
        }

        List<SessionRecordResponse> recordResponses = records.stream()
                .map(SessionRecordResponse::from)
                .toList();

        return new SessionStatsResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getTotalCards(),
                session.getCorrectCount(),
                accuracy,
                duration,
                recordResponses
        );
    }

    public record SessionRecordResponse(
            Long id,
            Long cardId,
            Long userCardId,
            String question,
            Boolean isCorrect,
            LocalDateTime studiedAt
    ) {

        public static SessionRecordResponse from(StudyRecord record) {
            String question = null;
            Long cardId = null;
            Long userCardId = null;

            if (record.getCard() != null) {
                cardId = record.getCard().getId();
                question = record.getCard().getQuestion();
            } else if (record.getUserCard() != null) {
                userCardId = record.getUserCard().getId();
                question = record.getUserCard().getQuestion();
            }

            return new SessionRecordResponse(
                    record.getId(),
                    cardId,
                    userCardId,
                    question,
                    record.getIsCorrect(),
                    record.getStudiedAt()
            );
        }
    }
}
