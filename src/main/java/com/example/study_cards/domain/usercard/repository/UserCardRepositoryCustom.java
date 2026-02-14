package com.example.study_cards.domain.usercard.repository;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserCardRepositoryCustom {

    List<UserCard> findByUserOrderByEfFactorAsc(User user);

    List<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category);

    List<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories);

    Page<UserCard> findByUserWithCategory(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoryWithCategory(User user, Category category, Pageable pageable);

    Page<UserCard> findByUserAndCategoriesWithCategory(User user, List<Category> categories, Pageable pageable);

    Page<UserCard> findByUserOrderByEfFactorAsc(User user, Pageable pageable);

    Page<UserCard> findByUserAndCategoryOrderByEfFactorAsc(User user, Category category, Pageable pageable);

    Page<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories, Pageable pageable);

    long countByUser(User user);

    long countByUserAndCategory(User user, Category category);

    long countByUserAndCategories(User user, List<Category> categories);

    Page<UserCard> searchByKeyword(User user, String keyword, List<Category> categories, Pageable pageable);
}
