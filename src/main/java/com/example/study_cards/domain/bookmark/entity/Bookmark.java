package com.example.study_cards.domain.bookmark.entity;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.common.audit.BaseEntity;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "bookmarks")
public class Bookmark extends BaseEntity {

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

    @Builder
    public Bookmark(User user, Card card, UserCard userCard) {
        this.user = user;
        this.card = card;
        this.userCard = userCard;
    }

    public boolean isForPublicCard() {
        return this.card != null;
    }

    public boolean isForUserCard() {
        return this.userCard != null;
    }
}
