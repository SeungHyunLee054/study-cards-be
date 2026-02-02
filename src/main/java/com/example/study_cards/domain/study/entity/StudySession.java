package com.example.study_cards.domain.study.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study_sessions")
public class StudySession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Integer totalCards;

    @Column(nullable = false)
    private Integer correctCount;

    @Builder
    public StudySession(User user) {
        this.user = user;
        this.startedAt = LocalDateTime.now();
        this.totalCards = 0;
        this.correctCount = 0;
    }

    public void endSession() {
        this.endedAt = LocalDateTime.now();
    }

    public void incrementTotalCards() {
        this.totalCards++;
    }

    public void incrementCorrectCount() {
        this.correctCount++;
    }
}
