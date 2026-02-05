package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.study.constant.SM2Constants;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
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
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_card_id")
    private UserCard userCard;

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

    @Column(nullable = false)
    private Integer interval;

    @Column(nullable = false)
    private Double efFactor;

    @Builder
    public StudyRecord(User user, Card card, UserCard userCard, StudySession session, Boolean isCorrect, LocalDate nextReviewDate, Integer interval, Double efFactor) {
        this.user = user;
        this.card = card;
        this.userCard = userCard;
        this.session = session;
        this.studiedAt = LocalDateTime.now();
        this.isCorrect = isCorrect;
        this.nextReviewDate = nextReviewDate;
        this.repetitionCount = 1;
        this.interval = interval != null ? interval : 1;
        this.efFactor = efFactor;
    }

    public boolean isForPublicCard() {
        return this.card != null;
    }

    public boolean isForUserCard() {
        return this.userCard != null;
    }

    public void updateEfFactor(boolean isCorrect) {
        this.efFactor = SM2Constants.calculateNewEfFactor(this.efFactor, isCorrect);
    }

    public void updateForReview(Boolean isCorrect, LocalDate newNextReviewDate, Integer newInterval) {
        this.studiedAt = LocalDateTime.now();
        this.isCorrect = isCorrect;
        this.nextReviewDate = newNextReviewDate;
        this.interval = newInterval;
        this.repetitionCount++;
    }
}
