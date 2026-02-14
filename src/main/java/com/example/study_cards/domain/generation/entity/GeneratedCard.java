package com.example.study_cards.domain.generation.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "generated_cards", indexes = {
        @Index(name = "idx_generated_card_status", columnList = "status"),
        @Index(name = "idx_generated_card_category", columnList = "category_id"),
        @Index(name = "idx_generated_card_model", columnList = "model")
})
public class GeneratedCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private String sourceWord;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String questionSub;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String answerSub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GenerationStatus status = GenerationStatus.PENDING;

    private LocalDateTime approvedAt;

    @Builder
    public GeneratedCard(String model, String sourceWord, String prompt, String question,
                         String questionSub, String answer, String answerSub, Category category) {
        this.model = model;
        this.sourceWord = sourceWord;
        this.prompt = prompt;
        this.question = question;
        this.questionSub = questionSub;
        this.answer = answer;
        this.answerSub = answerSub;
        this.category = category;
        this.status = GenerationStatus.PENDING;
    }

    public void approve() {
        this.status = GenerationStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = GenerationStatus.REJECTED;
        this.approvedAt = null;
    }

    public void markAsMigrated() {
        this.status = GenerationStatus.MIGRATED;
    }

    public boolean isPending() {
        return this.status == GenerationStatus.PENDING;
    }

    public boolean isApproved() {
        return this.status == GenerationStatus.APPROVED;
    }

    public Card toCard() {
        return Card.builder()
                .question(this.question)
                .questionSub(this.questionSub)
                .answer(this.answer)
                .answerSub(this.answerSub)
                .category(this.category)
                .aiGenerated(true)
                .build();
    }
}
