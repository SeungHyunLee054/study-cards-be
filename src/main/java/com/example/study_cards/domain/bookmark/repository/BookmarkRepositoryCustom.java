package com.example.study_cards.domain.bookmark.repository;

import com.example.study_cards.domain.bookmark.entity.Bookmark;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepositoryCustom {

    Page<Bookmark> findByUser(User user, List<Category> categories, Pageable pageable);

    boolean existsByUserAndCard(User user, Card card);

    boolean existsByUserAndUserCard(User user, UserCard userCard);

    Optional<Bookmark> findByUserAndCard(User user, Card card);

    Optional<Bookmark> findByUserAndUserCard(User user, UserCard userCard);

    long countByUser(User user);
}
