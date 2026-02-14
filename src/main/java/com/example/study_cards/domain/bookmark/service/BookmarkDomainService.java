package com.example.study_cards.domain.bookmark.service;

import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.bookmark.exception.BookmarkErrorCode;
import com.example.study_cards.domain.bookmark.exception.BookmarkException;
import com.example.study_cards.domain.bookmark.repository.BookmarkRepository;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BookmarkDomainService {

    private final BookmarkRepository bookmarkRepository;

    public Bookmark bookmarkCard(User user, Card card) {
        if (bookmarkRepository.existsByUserAndCard(user, card)) {
            throw new BookmarkException(BookmarkErrorCode.ALREADY_BOOKMARKED);
        }
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .card(card)
                .build();
        return bookmarkRepository.save(bookmark);
    }

    public Bookmark bookmarkUserCard(User user, UserCard userCard) {
        if (bookmarkRepository.existsByUserAndUserCard(user, userCard)) {
            throw new BookmarkException(BookmarkErrorCode.ALREADY_BOOKMARKED);
        }
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .userCard(userCard)
                .build();
        return bookmarkRepository.save(bookmark);
    }

    public void unbookmarkCard(User user, Card card) {
        Bookmark bookmark = bookmarkRepository.findByUserAndCard(user, card)
                .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
    }

    public void unbookmarkUserCard(User user, UserCard userCard) {
        Bookmark bookmark = bookmarkRepository.findByUserAndUserCard(user, userCard)
                .orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
        bookmarkRepository.delete(bookmark);
    }

    public boolean isCardBookmarked(User user, Card card) {
        return bookmarkRepository.existsByUserAndCard(user, card);
    }

    public boolean isUserCardBookmarked(User user, UserCard userCard) {
        return bookmarkRepository.existsByUserAndUserCard(user, userCard);
    }

    public Page<Bookmark> findBookmarks(User user, List<Category> categories, Pageable pageable) {
        return bookmarkRepository.findByUser(user, categories, pageable);
    }
}
