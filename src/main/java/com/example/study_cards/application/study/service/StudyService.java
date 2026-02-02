package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.domain.card.entity.Card;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.card.service.CardDomainService;
import com.example.study_cards.domain.study.entity.StudyRecord;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class StudyService {

    private final StudyDomainService studyDomainService;
    private final CardDomainService cardDomainService;

    public List<StudyCardResponse> getTodayCards(User user, Category category) {
        List<Card> cards = studyDomainService.findTodayStudyCards(user, category);
        return cards.stream()
                .map(StudyCardResponse::from)
                .toList();
    }

    @Transactional
    public StudyResultResponse submitAnswer(User user, StudyAnswerRequest request) {
        Card card = cardDomainService.findById(request.cardId());
        StudyRecord record = studyDomainService.processAnswer(user, card, null, request.isCorrect());

        user.updateStreak(LocalDate.now());

        return new StudyResultResponse(
                card.getId(),
                request.isCorrect(),
                record.getNextReviewDate(),
                record.getEfFactor()
        );
    }
}
