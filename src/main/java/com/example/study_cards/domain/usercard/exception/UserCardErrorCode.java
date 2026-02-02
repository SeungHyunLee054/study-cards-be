package com.example.study_cards.domain.usercard.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserCardErrorCode implements ErrorCode {

    USER_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 카드를 찾을 수 없습니다."),
    USER_CARD_NOT_OWNER(HttpStatus.FORBIDDEN, "해당 카드에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
