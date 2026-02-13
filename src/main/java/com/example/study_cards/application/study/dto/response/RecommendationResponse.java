package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.study.entity.StudyRecord;

import java.time.LocalDate;
import java.util.List;

public record RecommendationResponse(
        List<RecommendedCard> recommendations,
        int totalCount,
        String aiExplanation
) {
    public record RecommendedCard(
            Long cardId,
            Long userCardId,
            String question,
            String questionSub,
            int priorityScore,
            LocalDate nextReviewDate,
            Double efFactor,
            Boolean lastCorrect
    ) {
        public static RecommendedCard from(StudyRecord record, int score) {
            String question;
            String questionSub;
            Long cardId = null;
            Long userCardId = null;

            if (record.isForPublicCard()) {
                cardId = record.getCard().getId();
                question = record.getCard().getQuestion();
                questionSub = record.getCard().getQuestionSub();
            } else {
                userCardId = record.getUserCard().getId();
                question = record.getUserCard().getQuestion();
                questionSub = record.getUserCard().getQuestionSub();
            }

            return new RecommendedCard(
                    cardId,
                    userCardId,
                    question,
                    questionSub,
                    score,
                    record.getNextReviewDate(),
                    record.getEfFactor(),
                    record.getIsCorrect()
            );
        }
    }

    public static RecommendationResponse of(List<RecommendedCard> recommendations, String aiExplanation) {
        return new RecommendationResponse(recommendations, recommendations.size(), aiExplanation);
    }
}
