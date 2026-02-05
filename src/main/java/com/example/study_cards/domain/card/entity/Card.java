package com.example.study_cards.domain.card.entity;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_category", columnList = "category_id")
})
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String questionSub;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String answerSub;

    @Column(nullable = false)
    private Double efFactor;

    @Column(nullable = false)
    private Boolean aiGenerated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    public Card(String question, String questionSub, String answer, String answerSub,
                Double efFactor, Boolean aiGenerated, Category category) {
        this.question = question;
        this.questionSub = questionSub;
        this.answer = answer;
        this.answerSub = answerSub;
        this.efFactor = efFactor != null ? efFactor : 2.5;
        this.aiGenerated = aiGenerated != null ? aiGenerated : false;
        this.category = category;
    }

    public boolean isAiGenerated() {
        return Boolean.TRUE.equals(aiGenerated);
    }

    public void update(String question, String questionSub, String answer, String answerSub, Category category) {
        this.question = question;
        this.questionSub = questionSub;
        this.answer = answer;
        this.answerSub = answerSub;
        this.category = category;
    }
}
