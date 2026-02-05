package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.study.entity.StudySession;

import java.time.Duration;
import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer totalCards,
        Integer correctCount,
        Double accuracyRate,
        Long durationSeconds
) {

    public static SessionResponse from(StudySession session) {
        double accuracy = session.getTotalCards() > 0
                ? Math.round((double) session.getCorrectCount() / session.getTotalCards() * 1000.0) / 10.0
                : 0.0;

        Long duration = null;
        if (session.getEndedAt() != null) {
            duration = Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds();
        }

        return new SessionResponse(
                session.getId(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getTotalCards(),
                session.getCorrectCount(),
                accuracy,
                duration
        );
    }
}
