package com.example.study_cards.domain.card.exception;

import com.example.study_cards.common.exception.BaseException;
import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CardException extends BaseException {

    private final CardErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
