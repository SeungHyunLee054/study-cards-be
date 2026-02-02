package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.study.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
}
