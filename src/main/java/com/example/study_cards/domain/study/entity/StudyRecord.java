package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_records")
public class StudyRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private StudySession session;

    @Column(nullable = false)
    private LocalDateTime studiedAt;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column(nullable = false)
    private LocalDate nextReviewDate;

    @Column(nullable = false)
    private Integer repetitionCount;

    @Builder
    public StudyRecord(User user, Card card, StudySession session, Boolean isCorrect, LocalDate nextReviewDate) {
        this.user = user;
        this.card = card;
        this.session = session;
        this.studiedAt = LocalDateTime.now();
        this.isCorrect = isCorrect;
        this.nextReviewDate = nextReviewDate;
        this.repetitionCount = 1;
    }
}
