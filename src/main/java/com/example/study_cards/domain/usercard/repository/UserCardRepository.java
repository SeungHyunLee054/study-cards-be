package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCardRepository extends JpaRepository<UserCard, Long>, UserCardRepositoryCustom {

    List<UserCard> findByUser(User user);

    List<UserCard> findByUserAndCategory(User user, Category category);
}
