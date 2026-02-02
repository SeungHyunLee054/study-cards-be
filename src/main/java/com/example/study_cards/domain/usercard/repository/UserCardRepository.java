package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    List<UserCard> findByUser(User user);

    List<UserCard> findByUserAndCategory(User user, Category category);

    List<UserCard> findByUserOrderByEfFactorAsc(User user);

    List<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category);
}
