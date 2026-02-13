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
                category,
                false
        );

        // 관리자가 공개 카드를 추가하면 해당 카테고리의 마스터리 상태가 변경되므로 알림 초기화
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
        long totalCount = userCardCount + publicCardCount;

        long offset = pageable.getOffset();
        int size = pageable.getPageSize();
        List<CardResponse> content = new ArrayList<>();

        if (offset < userCardCount) {
            int userCardSize = (int) Math.min(size, userCardCount - offset);
            int userPageNum = (int) (offset / size);
            Page<UserCard> userCards = category != null
                    ? userCardDomainService.findByUserAndCategory(user, category, PageRequest.of(userPageNum, userCardSize).withSort(pageable.getSort()))
                    : userCardDomainService.findByUser(user, PageRequest.of(userPageNum, userCardSize).withSort(pageable.getSort()));
            content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());

            int remaining = size - content.size();
            if (remaining > 0) {
                Page<Card> publicCards = category != null
                        ? cardDomainService.findByCategory(category, PageRequest.of(0, remaining))
                        : cardDomainService.findAll(PageRequest.of(0, remaining));
                content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
            }
        } else {
            long publicCardOffset = offset - userCardCount;
            int publicPageNum = (int) (publicCardOffset / size);
            Page<Card> publicCards = category != null
                    ? cardDomainService.findByCategory(category, PageRequest.of(publicPageNum, size))
                    : cardDomainService.findAll(PageRequest.of(publicPageNum, size));
            content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
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
        long totalCount = userCardCount + publicCardCount;

        long offset = pageable.getOffset();
        int size = pageable.getPageSize();
        List<CardResponse> content = new ArrayList<>();

        if (offset < userCardCount) {
            int userCardSize = (int) Math.min(size, userCardCount - offset);
            int userPageNum = (int) (offset / size);
            Page<UserCard> userCards = category != null
                    ? userCardDomainService.findUserCardsForStudyByCategory(user, category, PageRequest.of(userPageNum, userCardSize))
                    : userCardDomainService.findUserCardsForStudy(user, PageRequest.of(userPageNum, userCardSize));
            content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());

            int remaining = size - content.size();
            if (remaining > 0) {
                Page<Card> publicCards = category != null
                        ? cardDomainService.findCardsForStudyByCategory(category, PageRequest.of(0, remaining))
                        : cardDomainService.findCardsForStudy(PageRequest.of(0, remaining));
                content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
            }
        } else {
            long publicCardOffset = offset - userCardCount;
            int publicPageNum = (int) (publicCardOffset / size);
            Page<Card> publicCards = category != null
                    ? cardDomainService.findCardsForStudyByCategory(category, PageRequest.of(publicPageNum, size))
                    : cardDomainService.findCardsForStudy(PageRequest.of(publicPageNum, size));
            content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
        }

        return new PageImpl<>(content, pageable, totalCount);
    }

    public Page<CardResponse> searchCards(Long userId, String keyword, String categoryCode, Pageable pageable) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new CardException(CardErrorCode.INVALID_SEARCH_KEYWORD);
        }
        String trimmedKeyword = keyword.trim();
        Category category = categoryCode != null ? categoryDomainService.findByCodeOrNull(categoryCode) : null;

        if (userId != null) {
            User user = userDomainService.findById(userId);

            long userCardTotal = userCardDomainService.searchByKeyword(user, trimmedKeyword, category, PageRequest.of(0, 1)).getTotalElements();
            long publicCardTotal = cardDomainService.searchByKeyword(trimmedKeyword, category, PageRequest.of(0, 1)).getTotalElements();
            long totalCount = userCardTotal + publicCardTotal;

            long offset = pageable.getOffset();
            int size = pageable.getPageSize();
            List<CardResponse> content = new ArrayList<>();

            if (offset < userCardTotal) {
                int userCardSize = (int) Math.min(size, userCardTotal - offset);
                int userPageNum = (int) (offset / size);
                Page<UserCard> userCards = userCardDomainService.searchByKeyword(user, trimmedKeyword, category, PageRequest.of(userPageNum, userCardSize));
                content.addAll(userCards.getContent().stream().map(CardResponse::fromUserCard).toList());

                int remaining = size - content.size();
                if (remaining > 0) {
                    Page<Card> publicCards = cardDomainService.searchByKeyword(trimmedKeyword, category, PageRequest.of(0, remaining));
                    content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
                }
            } else {
                long publicCardOffset = offset - userCardTotal;
                int publicPageNum = (int) (publicCardOffset / size);
                Page<Card> publicCards = cardDomainService.searchByKeyword(trimmedKeyword, category, PageRequest.of(publicPageNum, size));
                content.addAll(publicCards.getContent().stream().map(CardResponse::from).toList());
            }

            return new PageImpl<>(content, pageable, totalCount);
        }

        Page<Card> cards = cardDomainService.searchByKeyword(trimmedKeyword, category, pageable);
        return cards.map(CardResponse::from);
    }

    public long getCardCount(String categoryCode) {
        if (categoryCode != null) {
            Category category = categoryDomainService.findByCode(categoryCode);
            return cardDomainService.countByCategory(category);
        }
        return cardDomainService.count();
    }
}
