package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserCardRepositoryCustom {

    record CategoryCount(Long categoryId, String categoryCode, Long count) {}

    List<UserCard> findByUserOrderByEfFactorAsc(User user);

    List<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories);

    Page<UserCard> findByUserWithCategory(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoriesWithCategory(User user, List<Category> categories, Pageable pageable);

    Page<UserCard> findByUserOrderByEfFactorAsc(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories, Pageable pageable);

    long countByUser(User user);

    long countByUserAndCategories(User user, List<Category> categories);

    List<CategoryCount> countByUserGroupByCategory(User user);

    Page<UserCard> searchByKeyword(User user, String keyword, List<Category> categories, Pageable pageable);
}
