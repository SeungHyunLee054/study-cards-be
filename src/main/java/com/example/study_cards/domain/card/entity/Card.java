package com.example.study_cards.domain.card.entity;

import com.example.study_cards.domain.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cards")
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionEn;

    @Column(columnDefinition = "TEXT")
    private String questionKo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerEn;

    @Column(columnDefinition = "TEXT")
    private String answerKo;

    @Column(nullable = false)
    private Double efFactor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Builder
    public Card(String questionEn, String questionKo, String answerEn, String answerKo,
                Double efFactor, Category category) {
        this.questionEn = questionEn;
        this.questionKo = questionKo;
        this.answerEn = answerEn;
        this.answerKo = answerKo;
        this.efFactor = efFactor != null ? efFactor : 2.5;
        this.category = category;
    }

    public void update(String questionEn, String questionKo, String answerEn, String answerKo, Category category) {
        this.questionEn = questionEn;
        this.questionKo = questionKo;
        this.answerEn = answerEn;
        this.answerKo = answerKo;
        this.category = category;
    }
}
