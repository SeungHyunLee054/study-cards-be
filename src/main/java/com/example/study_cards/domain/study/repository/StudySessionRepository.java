package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.study.entity.StudySession;
import com.example.study_cards.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserOrderByStartedAtDesc(User user);
}
