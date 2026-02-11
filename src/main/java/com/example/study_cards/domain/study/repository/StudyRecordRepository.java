package com.example.study_cards.domain.study.repository;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long>, StudyRecordRepositoryCustom {

    Optional<StudyRecord> findByUserAndCard(User user, Card card);

    Optional<StudyRecord> findByUserAndUserCard(User user, UserCard userCard);

    boolean existsByCard(Card card);
}
