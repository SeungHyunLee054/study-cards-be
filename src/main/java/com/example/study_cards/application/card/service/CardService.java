package com.example.study_cards.application.card.service;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import com.example.study_cards.infra.redis.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CardService {

    private final CardDomainService cardDomainService;
    private final UserCardDomainService userCardDomainService;
    private final UserDomainService userDomainService;
    private final RateLimitService rateLimitService;

    public List<CardResponse> getCards() {
        return cardDomainService.findAll().stream()
                .map(CardResponse::from)
                .toList();
    }

    public List<CardResponse> getCardsByCategory(Category category) {
        return cardDomainService.findByCategory(category).stream()
                .map(CardResponse::from)
                .toList();
    }

    public CardResponse getCard(Long id) {
        return CardResponse.from(cardDomainService.findById(id));
    }

    public List<CardResponse> getCardsForStudy(Category category, boolean isAuthenticated, String ipAddress) {
        List<Card> cards;
        if (category != null) {
            cards = cardDomainService.findCardsForStudyByCategory(category);
        } else {
            cards = cardDomainService.findCardsForStudy();
        }

        if (!isAuthenticated) {
            int remainingCards = rateLimitService.getRemainingCards(ipAddress);
            if (remainingCards == 0) {
                throw new CardException(CardErrorCode.RATE_LIMIT_EXCEEDED);
            }

            int limitedSize = Math.min(cards.size(), remainingCards);
            cards = cards.subList(0, limitedSize);
            rateLimitService.incrementCardCount(ipAddress, cards.size());
        }

        return cards.stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        Card card = cardDomainService.createCard(
                request.questionEn(),
                request.questionKo(),
                request.answerEn(),
                request.answerKo(),
                request.category()
        );
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse updateCard(Long id, CardUpdateRequest request) {
        Card card = cardDomainService.updateCard(
                id,
                request.questionEn(),
                request.questionKo(),
                request.answerEn(),
                request.answerKo(),
                request.category()
        );
        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        cardDomainService.deleteCard(id);
    }

    public List<CardResponse> getAllCardsWithUserCards(Long userId, Category category) {
        User user = userDomainService.findById(userId);
        List<CardResponse> result = new ArrayList<>();

        List<Card> publicCards = category != null
                ? cardDomainService.findByCategory(category)
                : cardDomainService.findAll();
        result.addAll(publicCards.stream().map(CardResponse::from).toList());

        List<UserCard> userCards = category != null
                ? userCardDomainService.findByUserAndCategory(user, category)
                : userCardDomainService.findByUser(user);
        result.addAll(userCards.stream().map(CardResponse::fromUserCard).toList());

        return result;
    }

    public List<CardResponse> getCardsForStudyWithUserCards(Long userId, Category category) {
        User user = userDomainService.findById(userId);
        List<CardResponse> result = new ArrayList<>();

        List<Card> publicCards = category != null
                ? cardDomainService.findCardsForStudyByCategory(category)
                : cardDomainService.findCardsForStudy();
        result.addAll(publicCards.stream().map(CardResponse::from).toList());

        List<UserCard> userCards = category != null
                ? userCardDomainService.findUserCardsForStudyByCategory(user, category)
                : userCardDomainService.findUserCardsForStudy(user);
        result.addAll(userCards.stream().map(CardResponse::fromUserCard).toList());

        return result;
    }

    public long getCardCount(Category category) {
        if (category != null) {
            return cardDomainService.countByCategory(category);
        }
        return cardDomainService.count();
    }
}
