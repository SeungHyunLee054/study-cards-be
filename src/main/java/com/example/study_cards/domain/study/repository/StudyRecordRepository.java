package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {

    List<StudyRecord> findByUserAndNextReviewDateLessThanEqual(User user, LocalDate date);

    Optional<StudyRecord> findByUserAndCard(User user, Card card);
}
