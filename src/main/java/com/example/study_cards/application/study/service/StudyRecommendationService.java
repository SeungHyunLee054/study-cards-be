package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.response.CategoryAccuracyResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse;
import com.example.study_cards.application.study.dto.response.RecommendationResponse.RecommendedCard;
import com.example.study_cards.domain.study.repository.StudyRecordRepositoryCustom.CategoryAccuracy;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.study.service.StudyDomainService.ScoredRecord;
import com.example.study_cards.domain.subscription.entity.SubscriptionPlan;
import com.example.study_cards.domain.subscription.service.SubscriptionDomainService;
import com.example.study_cards.domain.user.entity.User;
import com.example.study_cards.infra.ai.service.AiGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 20;

    private final StudyDomainService studyDomainService;
    private final SubscriptionDomainService subscriptionDomainService;
    private final AiGenerationService aiGenerationService;

    public RecommendationResponse getRecommendations(User user, int limit) {
        SubscriptionPlan plan = subscriptionDomainService.getEffectivePlan(user);

        List<ScoredRecord> scoredRecords = studyDomainService.findPrioritizedDueRecords(user, limit);

        List<RecommendedCard> recommendations = scoredRecords.stream()
                .map(sr -> RecommendedCard.from(sr.record(), sr.score()))
                .toList();

        String aiExplanation = null;
        if (plan.isCanUseAiRecommendations() && !recommendations.isEmpty()) {
            aiExplanation = generateAiExplanation(user, recommendations);
        }

        return RecommendationResponse.of(recommendations, aiExplanation);
    }

    public RecommendationResponse getRecommendations(User user) {
        return getRecommendations(user, DEFAULT_RECOMMENDATION_LIMIT);
    }

    public List<CategoryAccuracyResponse> getCategoryAccuracy(User user) {
        return studyDomainService.calculateCategoryAccuracy(user).stream()
                .map(CategoryAccuracyResponse::from)
                .toList();
    }

    private String generateAiExplanation(User user, List<RecommendedCard> recommendations) {
        try {
            List<CategoryAccuracy> accuracies = studyDomainService.calculateCategoryAccuracy(user);

            StringBuilder prompt = new StringBuilder();
            prompt.append("다음 학습 데이터를 분석하여 한국어로 1~2문장의 간결한 복습 추천 메시지를 작성하세요.\n\n");

            if (!accuracies.isEmpty()) {
                prompt.append("카테고리별 정답률:\n");
                for (CategoryAccuracy ca : accuracies) {
                    prompt.append("- ").append(ca.categoryName()).append(": ")
                            .append(ca.accuracy()).append("%\n");
                }
            }

            prompt.append("\n오늘 복습 추천 카드 ").append(recommendations.size()).append("개 중:\n");
            int highPriorityCount = (int) recommendations.stream()
                    .filter(r -> r.priorityScore() >= 500).count();
            int wrongCount = (int) recommendations.stream()
                    .filter(r -> r.lastCorrect() != null && !r.lastCorrect()).count();

            prompt.append("- 긴급 복습 필요: ").append(highPriorityCount).append("개\n");
            prompt.append("- 최근 오답: ").append(wrongCount).append("개\n");
            prompt.append("\n예시: '최근 네트워크 개념에서 연속 오답이 발생했습니다. TCP/IP 관련 카드를 우선 복습하세요.'\n");
            prompt.append("메시지만 출력하세요. 부가 설명 없이 핵심만 작성하세요.");

            return aiGenerationService.generateContent(prompt.toString());
        } catch (Exception e) {
            log.warn("[AI] 복습 추천 메시지 생성 실패: {}", e.getMessage());
            return null;
        }
    }
}
