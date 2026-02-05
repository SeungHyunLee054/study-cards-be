package com.example.study_cards.domain.usercard.entity;

import com.example.study_cards.domain.category.entity.Category;
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
@Table(name = "user_cards", indexes = {
        @Index(name = "idx_user_card_user", columnList = "user_id"),
        @Index(name = "idx_user_card_category", columnList = "category_id")
})
public class UserCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Version
    private Long version;

    @Builder
    public UserCard(User user, String question, String questionSub, String answer, String answerSub,
                    Double efFactor, Category category) {
        this.user = user;
        this.question = question;
        this.questionSub = questionSub;
        this.answer = answer;
        this.answerSub = answerSub;
        this.efFactor = efFactor != null ? efFactor : 2.5;
        this.category = category;
    }

    public void update(String question, String questionSub, String answer, String answerSub, Category category) {
        this.question = question;
        this.questionSub = questionSub;
        this.answer = answer;
        this.answerSub = answerSub;
        this.category = category;
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
