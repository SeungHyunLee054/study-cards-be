package com.example.study_cards.application.usercard.service;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
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
public class UserCardService {

    private final UserCardDomainService userCardDomainService;
    private final UserDomainService userDomainService;
    private final CategoryDomainService categoryDomainService;

    public Page<UserCardResponse> getUserCards(Long userId, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Page<UserCard> userCards = userCardDomainService.findByUser(user, pageable);
        return userCards.map(UserCardResponse::from);
    }

    public Page<UserCardResponse> getUserCardsByCategory(Long userId, String categoryCode, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Category category = categoryDomainService.findByCode(categoryCode);
        Page<UserCard> userCards = userCardDomainService.findByUserAndCategory(user, category, pageable);
        return userCards.map(UserCardResponse::from);
    }

    public UserCardResponse getUserCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.findByIdAndValidateOwner(cardId, user);
        return UserCardResponse.from(userCard);
    }

    public Page<UserCardResponse> getUserCardsForStudy(Long userId, String categoryCode, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;

        Page<UserCard> userCards;
        if (category != null) {
            userCards = userCardDomainService.findUserCardsForStudyByCategory(user, category, pageable);
        } else {
            userCards = userCardDomainService.findUserCardsForStudy(user, pageable);
        }
        return userCards.map(UserCardResponse::from);
    }

    @Transactional
    public UserCardResponse createUserCard(Long userId, UserCardCreateRequest request) {
        User user = userDomainService.findById(userId);
        Category category = categoryDomainService.findByCode(request.category());
        UserCard userCard = userCardDomainService.createUserCard(
                user,
                request.question(),
                request.questionSub(),
                request.answer(),
                request.answerSub(),
                category
        );
        return UserCardResponse.from(userCard);
    }

    @Transactional
    public UserCardResponse updateUserCard(Long userId, Long cardId, UserCardUpdateRequest request) {
        User user = userDomainService.findById(userId);
        Category category = categoryDomainService.findByCode(request.category());
        UserCard userCard = userCardDomainService.updateUserCard(
                cardId,
                user,
                request.question(),
                request.questionSub(),
                request.answer(),
                request.answerSub(),
                category
        );
        return UserCardResponse.from(userCard);
    }

    @Transactional
    public void deleteUserCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        userCardDomainService.deleteUserCard(cardId, user);
    }
}
