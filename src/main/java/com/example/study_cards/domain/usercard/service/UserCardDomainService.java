package com.example.study_cards.domain.usercard.service;

import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.exception.UserCardErrorCode;
import com.example.study_cards.domain.usercard.exception.UserCardException;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import com.example.study_cards.domain.usercard.repository.UserCardRepositoryCustom.CategoryCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class UserCardDomainService {

    private final UserCardRepository userCardRepository;

    public UserCard createUserCard(User user, String question, String questionSub,
                                   String answer, String answerSub, Category category) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(question, "question must not be null");
        Objects.requireNonNull(answer, "answer must not be null");

        UserCard userCard = UserCard.builder()
                .user(user)
                .question(question)
                .questionSub(questionSub)
                .answer(answer)
                .answerSub(answerSub)
                .category(category)
                .build();
        return userCardRepository.save(userCard);
    }

    public UserCard findById(Long id) {
        return userCardRepository.findById(id)
                .orElseThrow(() -> new UserCardException(UserCardErrorCode.USER_CARD_NOT_FOUND));
    }

    public UserCard findByIdAndValidateOwner(Long id, User user) {
        UserCard userCard = findById(id);
        if (!userCard.isOwnedBy(user)) {
            throw new UserCardException(UserCardErrorCode.USER_CARD_NOT_OWNER);
        }
        return userCard;
    }

    public List<UserCard> findByUser(User user) {
        return userCardRepository.findByUser(user);
    }

    public List<UserCard> findByUserOrderByEfFactorAsc(User user) {
        return userCardRepository.findByUserOrderByEfFactorAsc(user);
    }

    public List<UserCard> findByUserAndCategoriesOrderByEfFactorAsc(User user, List<Category> categories) {
        return userCardRepository.findByUserAndCategoriesOrderByEfFactorAsc(user, categories);
    }

    public UserCard updateUserCard(Long id, User user, String question, String questionSub,
                                   String answer, String answerSub, Category category) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(question, "question must not be null");
        Objects.requireNonNull(answer, "answer must not be null");

        UserCard userCard = findByIdAndValidateOwner(id, user);
        userCard.update(question, questionSub, answer, answerSub, category);
        return userCard;
    }

    public List<UserCard> saveAll(List<UserCard> userCards) {
        return userCardRepository.saveAll(userCards);
    }

    public void deleteUserCard(Long id, User user) {
        UserCard userCard = findByIdAndValidateOwner(id, user);
        userCardRepository.delete(userCard);
    }

    public Page<UserCard> findByUser(User user, Pageable pageable) {
        return userCardRepository.findByUserWithCategory(user, pageable);
    }

    public Page<UserCard> findByUserAndCategories(User user, List<Category> categories, Pageable pageable) {
        return userCardRepository.findByUserAndCategoriesWithCategory(user, categories, pageable);
    }

    public Page<UserCard> findUserCardsForStudy(User user, Pageable pageable) {
        return userCardRepository.findByUserOrderByEfFactorAsc(user, pageable);
    }

    public Page<UserCard> findUserCardsForStudyByCategories(User user, List<Category> categories, Pageable pageable) {
        return userCardRepository.findByUserAndCategoriesOrderByEfFactorAsc(user, categories, pageable);
    }

    public Page<UserCard> searchByKeyword(User user, String keyword, List<Category> categories, Pageable pageable) {
        return userCardRepository.searchByKeyword(user, keyword, categories, pageable);
    }

    public long countByUser(User user) {
        return userCardRepository.countByUser(user);
    }

    public long countByUserAndCategories(User user, List<Category> categories) {
        return userCardRepository.countByUserAndCategories(user, categories);
    }

    public List<CategoryCount> countByUserGroupByCategory(User user) {
        return userCardRepository.countByUserGroupByCategory(user);
    }
}
