package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.study.exception.StudyErrorCode;
import com.example.study_cards.domain.study.exception.StudyException;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudySessionDomainService {

    private final StudySessionRepository studySessionRepository;

    public StudySession createSession(User user) {
        StudySession session = StudySession.builder()
                .user(user)
                .build();
        return studySessionRepository.save(session);
    }

    public StudySession findSessionById(Long sessionId) {
        return studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new StudyException(StudyErrorCode.SESSION_NOT_FOUND));
    }

    public Optional<StudySession> findActiveSession(User user) {
        return studySessionRepository.findByUserAndEndedAtIsNull(user);
    }

    public Page<StudySession> findSessionHistory(User user, Pageable pageable) {
        return studySessionRepository.findByUserOrderByStartedAtDesc(user, pageable);
    }

    public void validateSessionOwnership(StudySession session, User user) {
        if (!session.getUser().getId().equals(user.getId())) {
            throw new StudyException(StudyErrorCode.SESSION_ACCESS_DENIED);
        }
    }
}
