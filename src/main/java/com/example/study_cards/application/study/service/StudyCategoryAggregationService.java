package com.example.study_cards.application.study.service;

import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.domain.usercard.service.UserCardDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyCategoryAggregationService {

    private final CardDomainService cardDomainService;
    private final UserCardDomainService userCardDomainService;

    public Map<String, Long> countTotalCardsByCategoryWithUserCards(User user) {
        Map<String, Long> totalByCategory = new LinkedHashMap<>();

        for (var row : cardDomainService.countAllByCategory()) {
            totalByCategory.merge(row.categoryCode(), row.count(), Long::sum);
        }
        for (var row : userCardDomainService.countByUserGroupByCategory(user)) {
            totalByCategory.merge(row.categoryCode(), row.count(), Long::sum);
        }

        return totalByCategory;
    }
}

