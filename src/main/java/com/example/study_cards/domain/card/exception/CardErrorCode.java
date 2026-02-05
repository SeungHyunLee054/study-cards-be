package com.example.study_cards.domain.card.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardErrorCode implements ErrorCode {

    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리입니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "일일 학습 한도를 초과했습니다."),
    CARD_HAS_STUDY_RECORDS(HttpStatus.CONFLICT, "학습 기록이 존재하는 카드는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
