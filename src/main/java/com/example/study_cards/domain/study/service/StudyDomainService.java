package com.example.study_cards.domain.study.service;

import com.example.study_cards.domain.study.repository.StudyRecordRepository;
import com.example.study_cards.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class StudyDomainService {

    private final StudySessionRepository studySessionRepository;
    private final StudyRecordRepository studyRecordRepository;

    // TODO: 학습 세션 생성

    // TODO: 학습 기록 저장

    // TODO: 오늘 복습할 카드 조회

    // TODO: Anki 알고리즘 (efFactor, nextReviewDate 계산)
}
