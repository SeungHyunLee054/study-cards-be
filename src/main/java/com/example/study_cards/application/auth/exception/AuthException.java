package com.example.study_cards.application.auth.exception;

import com.example.study_cards.common.exception.BaseException;
import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthException extends BaseException {

    private final AuthErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
