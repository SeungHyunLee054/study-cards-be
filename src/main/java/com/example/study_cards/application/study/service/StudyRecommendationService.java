package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.CategoryAccuracyResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse.RecommendedCard;
import com.example.study_cards.domain.study.service.StudyRecordDomainService;
import com.example.study_cards.domain.study.service.StudyRecordDomainService.ScoredRecord;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 20;

    private final StudyRecordDomainService studyRecordDomainService;

    public RecommendationResponse getRecommendations(User user, int limit) {
        List<ScoredRecord> scoredRecords = studyRecordDomainService.findPrioritizedDueRecords(user, limit);

        List<RecommendedCard> recommendations = scoredRecords.stream()
                .map(sr -> RecommendedCard.from(sr.record(), sr.score()))
                .toList();

        return RecommendationResponse.of(recommendations, buildRecommendationExplanation(recommendations));
    }

    public RecommendationResponse getRecommendations(User user) {
        return getRecommendations(user, DEFAULT_RECOMMENDATION_LIMIT);
    }

    public List<CategoryAccuracyResponse> getCategoryAccuracy(User user) {
        return studyRecordDomainService.calculateCategoryAccuracy(user).stream()
                .map(CategoryAccuracyResponse::from)
                .toList();
    }

    private String buildRecommendationExplanation(List<RecommendedCard> recommendations) {
        if (recommendations.isEmpty()) {
            return null;
        }

        int topCount = Math.min(5, recommendations.size());
        long urgentCount = recommendations.stream()
                .filter(card -> card.priorityScore() >= 500)
                .count();
        long recentWrongCount = recommendations.stream()
                .filter(card -> Boolean.FALSE.equals(card.lastCorrect()))
                .count();

        if (urgentCount > 0) {
            return "우선순위가 높은 카드 " + urgentCount + "개를 먼저 복습하고 상위 "
                    + topCount + "개를 완료하세요.";
        }

        if (recentWrongCount > 0) {
            return "최근 오답 카드 " + recentWrongCount + "개를 먼저 다시 풀고 상위 "
                    + topCount + "개를 복습하세요.";
        }

        return "추천 카드 상위 " + topCount + "개를 순서대로 복습하세요.";
    }
}
