package com.example.study_cards.domain.usercard.service;

import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.exception.UserCardErrorCode;
import com.example.study_cards.domain.usercard.exception.UserCardException;
import com.example.study_cards.domain.usercard.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserCardDomainService {

    private final UserCardRepository userCardRepository;

    public UserCard createUserCard(User user, String questionEn, String questionKo,
                                   String answerEn, String answerKo, Category category) {
        UserCard userCard = UserCard.builder()
                .user(user)
                .questionEn(questionEn)
                .questionKo(questionKo)
                .answerEn(answerEn)
                .answerKo(answerKo)
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

    public List<UserCard> findByUserAndCategory(User user, Category category) {
        return userCardRepository.findByUserAndCategory(user, category);
    }

    public List<UserCard> findUserCardsForStudy(User user) {
        return userCardRepository.findByUserOrderByEfFactorAsc(user);
    }

    public List<UserCard> findUserCardsForStudyByCategory(User user, Category category) {
        return userCardRepository.findByUserAndCategoryOrderByEfFactorAsc(user, category);
    }

    public UserCard updateUserCard(Long id, User user, String questionEn, String questionKo,
                                   String answerEn, String answerKo, Category category) {
        UserCard userCard = findByIdAndValidateOwner(id, user);
        userCard.update(questionEn, questionKo, answerEn, answerKo, category);
        return userCard;
    }

    public void deleteUserCard(Long id, User user) {
        UserCard userCard = findByIdAndValidateOwner(id, user);
        userCardRepository.delete(userCard);
    }
}
