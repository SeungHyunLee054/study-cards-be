package com.example.study_cards.application.bookmark.service;

import com.example.study_cards.application.bookmark.dto.response.BookmarkResponse;
import com.example.study_cards.application.bookmark.dto.response.BookmarkStatusResponse;
import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.bookmark.service.BookmarkDomainService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkDomainService bookmarkDomainService;
    private final CardDomainService cardDomainService;
    private final UserCardDomainService userCardDomainService;
    private final UserDomainService userDomainService;
    private final CategoryDomainService categoryDomainService;

    @Transactional
    public BookmarkResponse bookmarkCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        Card card = cardDomainService.findById(cardId);
        Bookmark bookmark = bookmarkDomainService.bookmarkCard(user, card);
        return BookmarkResponse.from(bookmark);
    }

    @Transactional
    public void unbookmarkCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        Card card = cardDomainService.findById(cardId);
        bookmarkDomainService.unbookmarkCard(user, card);
    }

    @Transactional
    public BookmarkResponse bookmarkUserCard(Long userId, Long userCardId) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.findByIdAndValidateOwner(userCardId, user);
        Bookmark bookmark = bookmarkDomainService.bookmarkUserCard(user, userCard);
        return BookmarkResponse.from(bookmark);
    }

    @Transactional
    public void unbookmarkUserCard(Long userId, Long userCardId) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.findByIdAndValidateOwner(userCardId, user);
        bookmarkDomainService.unbookmarkUserCard(user, userCard);
    }

    public Page<BookmarkResponse> getBookmarks(Long userId, String categoryCode, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;
        Page<Bookmark> bookmarks = bookmarkDomainService.findBookmarks(user, category, pageable);
        return bookmarks.map(BookmarkResponse::from);
    }

    public BookmarkStatusResponse getCardBookmarkStatus(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        Card card = cardDomainService.findById(cardId);
        return new BookmarkStatusResponse(bookmarkDomainService.isCardBookmarked(user, card));
    }

    public BookmarkStatusResponse getUserCardBookmarkStatus(Long userId, Long userCardId) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.findByIdAndValidateOwner(userCardId, user);
        return new BookmarkStatusResponse(bookmarkDomainService.isUserCardBookmarked(user, userCard));
    }
}
