package com.example.study_cards.application.card.service;

import com.example.study_cards.application.card.dto.request.CardCreateRequest;
import com.example.study_cards.application.card.dto.request.CardUpdateRequest;
import com.example.study_cards.application.card.dto.response.CardResponse;
import com.example.study_cards.application.notification.service.NotificationService;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.exception.CardErrorCode;
import com.example.study_cards.domain.card.exception.CardException;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.category.entity.Category;
import com.example.study_cards.domain.category.service.CategoryDomainService;
import com.example.study_cards.domain.notification.entity.NotificationType;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.user.service.UserDomainService;
import com.example.study_cards.domain.usercard.entity.UserCard;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import com.example.study_cards.infra.redis.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final CategoryDomainService categoryDomainService;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;

    public Page<CardResponse> getCards(Pageable pageable) {
        Page<Card> cards = cardDomainService.findAll(pageable);
        return cards.map(CardResponse::from);
    }

    public Page<CardResponse> getCardsByCategory(String categoryCode, Pageable pageable) {
        Category category = categoryDomainService.findByCode(categoryCode);
        Page<Card> cards = cardDomainService.findByCategory(category, pageable);
        return cards.map(CardResponse::from);
    }

    public CardResponse getCard(Long id) {
        return CardResponse.from(cardDomainService.findById(id));
    }

    public Page<CardResponse> getCardsForStudy(String categoryCode, boolean isAuthenticated, String ipAddress, Pageable pageable) {
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;

        Page<Card> cards;
        if (category != null) {
            cards = cardDomainService.findCardsForStudyByCategory(category, pageable);
        } else {
            cards = cardDomainService.findCardsForStudy(pageable);
        }

        if (!isAuthenticated) {
            int remainingCards = rateLimitService.getRemainingCards(ipAddress);
            if (remainingCards == 0) {
                throw new CardException(CardErrorCode.RATE_LIMIT_EXCEEDED);
            }

            List<Card> content = cards.getContent();
            int limitedSize = Math.min(content.size(), remainingCards);
            List<Card> limitedContent = content.subList(0, limitedSize);
            rateLimitService.incrementCardCount(ipAddress, limitedContent.size());

            return new PageImpl<>(
                    limitedContent.stream().map(CardResponse::from).toList(),
                    pageable,
                    limitedSize
            );
        }

        return cards.map(CardResponse::from);
    }

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        Category category = categoryDomainService.findByCode(request.category());
        Card card = cardDomainService.createCard(
                request.question(),
                request.questionSub(),
                request.answer(),
                request.answerSub(),
                category
        );

        notificationService.deleteNotificationsByTypeAndReference(
                NotificationType.CATEGORY_MASTERED,
                category.getId()
        );

        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse updateCard(Long id, CardUpdateRequest request) {
        Category category = categoryDomainService.findByCode(request.category());
        Card card = cardDomainService.updateCard(
                id,
                request.question(),
                request.questionSub(),
                request.answer(),
                request.answerSub(),
                category
        );
        return CardResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        cardDomainService.deleteCard(id);
    }

    public Page<CardResponse> getAllCardsWithUserCards(Long userId, String categoryCode, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;

        long publicCardCount = category != null
                ? cardDomainService.countByCategory(category)
                : cardDomainService.count();
        long userCardCount = category != null
                ? userCardDomainService.countByUserAndCategory(user, category)
                : userCardDomainService.countByUser(user);
        long totalCount = publicCardCount + userCardCount;

        long offset = pageable.getOffset();
        int size = pageable.getPageSize();
        List<CardResponse> content = new ArrayList<>();

        if (offset < publicCardCount) {
            int publicOffset = (int) offset;
            int publicSize = (int) Math.min(size, publicCardCount - offset);
            Page<Card> publicCards = category != null
                    ? cardDomainService.findByCategory(category, PageRequest.of(0, publicSize).withSort(pageable.getSort()))
                    : cardDomainService.findAll(PageRequest.of(0, publicSize).withSort(pageable.getSort()));

            List<Card> publicContent = publicCards.getContent();
            if (publicOffset > 0 && publicContent.size() > publicOffset) {
                publicContent = publicContent.subList(publicOffset, publicContent.size());
            }
            content.addAll(publicContent.stream().map(CardResponse::from).toList());

            int remaining = size - content.size();
            if (remaining > 0) {
                Page<UserCard> userCards = category != null
                        ? userCardDomainService.findByUserAndCategory(user, category, PageRequest.of(0, remaining))
                        : userCardDomainService.findByUser(user, PageRequest.of(0, remaining));
                content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());
            }
        } else {
            long userCardOffset = offset - publicCardCount;
            int userPageNum = (int) (userCardOffset / size);
            Page<UserCard> userCards = category != null
                    ? userCardDomainService.findByUserAndCategory(user, category, PageRequest.of(userPageNum, size))
                    : userCardDomainService.findByUser(user, PageRequest.of(userPageNum, size));
            content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());
        }

        return new PageImpl<>(content, pageable, totalCount);
    }

    public Page<CardResponse> getCardsForStudyWithUserCards(Long userId, String categoryCode, Pageable pageable) {
        User user = userDomainService.findById(userId);
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;

        long publicCardCount = category != null
                ? cardDomainService.countByCategory(category)
                : cardDomainService.count();
        long userCardCount = category != null
                ? userCardDomainService.countByUserAndCategory(user, category)
                : userCardDomainService.countByUser(user);
        long totalCount = publicCardCount + userCardCount;

        long offset = pageable.getOffset();
        int size = pageable.getPageSize();
        List<CardResponse> content = new ArrayList<>();

        if (offset < publicCardCount) {
            int publicSize = (int) Math.min(size, publicCardCount - offset);
            Page<Card> publicCards = category != null
                    ? cardDomainService.findCardsForStudyByCategory(category, PageRequest.of((int)(offset / size), publicSize))
                    : cardDomainService.findCardsForStudy(PageRequest.of((int)(offset / size), publicSize));
            content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());

            int remaining = size - content.size();
            if (remaining > 0) {
                Page<UserCard> userCards = category != null
                        ? userCardDomainService.findUserCardsForStudyByCategory(user, category, PageRequest.of(0, remaining))
                        : userCardDomainService.findUserCardsForStudy(user, PageRequest.of(0, remaining));
                content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());
            }
        } else {
            long userCardOffset = offset - publicCardCount;
            int userPageNum = (int) (userCardOffset / size);
            Page<UserCard> userCards = category != null
                    ? userCardDomainService.findUserCardsForStudyByCategory(user, category, PageRequest.of(userPageNum, size))
                    : userCardDomainService.findUserCardsForStudy(user, PageRequest.of(userPageNum, size));
            content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());
        }

        return new PageImpl<>(content, pageable, totalCount);
    }

    public long getCardCount(String categoryCode) {
        if (categoryCode != null) {
            Category category = categoryDomainService.findByCode(categoryCode);
            return cardDomainService.countByCategory(category);
        }
        return cardDomainService.count();
    }
}
