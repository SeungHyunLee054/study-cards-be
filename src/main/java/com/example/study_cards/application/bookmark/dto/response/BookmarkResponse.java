package com.example.study_cards.application.bookmark.dto.response;

import com.example.study_cards.application.card.dto.response.CardType;
import com.example.study_cards.application.category.dto.response.CategoryResponse;
import com.example.study_cards.domain.bookmark.entity.Bookmark;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long bookmarkId,
        Long cardId,
        CardType cardType,
        String question,
        String questionSub,
        String answer,
        String answerSub,
        CategoryResponse category,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkResponse from(Bookmark bookmark) {
        if (bookmark.isForPublicCard()) {
            var card = bookmark.getCard();
            return new BookmarkResponse(
                    bookmark.getId(),
                    card.getId(),
                    CardType.PUBLIC,
                    card.getQuestion(),
                    card.getQuestionSub(),
                    card.getAnswer(),
                    card.getAnswerSub(),
                    CategoryResponse.from(card.getCategory()),
                    bookmark.getCreatedAt()
            );
        } else {
            var userCard = bookmark.getUserCard();
            return new BookmarkResponse(
                    bookmark.getId(),
                    userCard.getId(),
                    CardType.CUSTOM,
                    userCard.getQuestion(),
                    userCard.getQuestionSub(),
                    userCard.getAnswer(),
                    userCard.getAnswerSub(),
                    CategoryResponse.from(userCard.getCategory()),
                    bookmark.getCreatedAt()
            );
        }
    }
}
