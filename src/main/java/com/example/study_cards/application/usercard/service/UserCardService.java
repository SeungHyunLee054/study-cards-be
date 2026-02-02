package com.example.study_cards.application.usercard.service;

import com.example.study_cards.application.usercard.dto.request.UserCardCreateRequest;
import com.example.study_cards.application.usercard.dto.request.UserCardUpdateRequest;
import com.example.study_cards.application.usercard.dto.response.UserCardResponse;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserCardService {

    private final UserCardDomainService userCardDomainService;
    private final UserDomainService userDomainService;

    public List<UserCardResponse> getUserCards(Long userId) {
        User user = userDomainService.findById(userId);
        return userCardDomainService.findByUser(user).stream()
                .map(UserCardResponse::from)
                .toList();
    }

    public List<UserCardResponse> getUserCardsByCategory(Long userId, Category category) {
        User user = userDomainService.findById(userId);
        return userCardDomainService.findByUserAndCategory(user, category).stream()
                .map(UserCardResponse::from)
                .toList();
    }

    public UserCardResponse getUserCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.findByIdAndValidateOwner(cardId, user);
        return UserCardResponse.from(userCard);
    }

    public List<UserCardResponse> getUserCardsForStudy(Long userId, Category category) {
        User user = userDomainService.findById(userId);
        List<UserCard> userCards;
        if (category != null) {
            userCards = userCardDomainService.findUserCardsForStudyByCategory(user, category);
        } else {
            userCards = userCardDomainService.findUserCardsForStudy(user);
        }
        return userCards.stream()
                .map(UserCardResponse::from)
                .toList();
    }

    @Transactional
    public UserCardResponse createUserCard(Long userId, UserCardCreateRequest request) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.createUserCard(
                user,
                request.questionEn(),
                request.questionKo(),
                request.answerEn(),
                request.answerKo(),
                request.category()
        );
        return UserCardResponse.from(userCard);
    }

    @Transactional
    public UserCardResponse updateUserCard(Long userId, Long cardId, UserCardUpdateRequest request) {
        User user = userDomainService.findById(userId);
        UserCard userCard = userCardDomainService.updateUserCard(
                cardId,
                user,
                request.questionEn(),
                request.questionKo(),
                request.answerEn(),
                request.answerKo(),
                request.category()
        );
        return UserCardResponse.from(userCard);
    }

    @Transactional
    public void deleteUserCard(Long userId, Long cardId) {
        User user = userDomainService.findById(userId);
        userCardDomainService.deleteUserCard(cardId, user);
    }
}
