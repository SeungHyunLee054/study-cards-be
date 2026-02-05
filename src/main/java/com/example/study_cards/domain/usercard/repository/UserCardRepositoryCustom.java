package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserCardRepositoryCustom {

    List<UserCard> findByUserOrderByEfFactorAsc(User user);

    List<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category);

    List<UserCard> findByUserWithCategory(User user);

    Optional<UserCard> findByIdWithCategory(Long id);

    Page<UserCard> findByUserWithCategory(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoryWithCategory(User user, Category category, Pageable pageable);

    Page<UserCard> findByUserOrderByEfFactorAsc(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category, Pageable pageable);

    long countByUser(User user);

    long countByUserAndCategory(User user, Category category);
}
