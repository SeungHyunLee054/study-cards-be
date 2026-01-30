package com.example.study_cards.application.study.service;

import com.example.study_cards.application.study.dto.request.StudyAnswerRequest;
import com.example.study_cards.application.study.dto.response.StudyCardResponse;
import com.example.study_cards.application.study.dto.response.StudyResultResponse;
import com.example.study_cards.domain.card.entity.Category;
import com.example.study_cards.domain.study.service.StudyDomainService;
import com.example.study_cards.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class StudyService {

    private final StudyDomainService studyDomainService;

    @Transactional(readOnly = true)
    public List<StudyCardResponse> getTodayCards(User user, Category category) {
        // TODO: studyDomainService 호출
        return null;
    }

    @Transactional
    public StudyResultResponse submitAnswer(User user, StudyAnswerRequest request) {
        // TODO: studyDomainService 호출
        return null;
    }
}
