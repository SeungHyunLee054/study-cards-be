package com.example.study_cards.application.study.dto.response;

import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;

public record StudyCardResponse(
        Long id,
        String questionEn,
        String questionKo,
        String answerEn,
        String answerKo,
        Category category
) {
    public static StudyCardResponse from(Card card) {
        return new StudyCardResponse(
                card.getId(),
                card.getQuestionEn(),
                card.getQuestionKo(),
                card.getAnswerEn(),
                card.getAnswerKo(),
                card.getCategory()
        );
    }
}
