package com.example.study_cards.domain.study.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StudyErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "이미 종료된 학습 세션입니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 학습 세션에 접근 권한이 없습니다."),
    NO_ACTIVE_SESSION(HttpStatus.NOT_FOUND, "활성화된 학습 세션이 없습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "학습 기록을 찾을 수 없습니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "일일 학습 한도를 초과했습니다."),
    AI_CARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AI 생성 카드에 접근하려면 Premium 구독이 필요합니다.");

    private final HttpStatus status;
    private final String message;
}
