package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findByUserAndEndedAtIsNull(User user);

    Page<StudySession> findByUserOrderByStartedAtDesc(User user, Pageable pageable);
}
