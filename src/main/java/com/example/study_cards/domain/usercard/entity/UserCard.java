package com.example.study_cards.domain.usercard.entity;

import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_cards")
public class UserCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    public UserCard(User user, String questionEn, String questionKo, String answerEn, String answerKo,
                    Double efFactor, Category category) {
        this.user = user;
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

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
