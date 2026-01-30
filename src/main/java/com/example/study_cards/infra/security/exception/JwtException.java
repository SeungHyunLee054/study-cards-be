package com.example.study_cards.infra.security.exception;

import com.example.study_cards.common.exception.BaseException;
import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JwtException extends BaseException {

    private final JwtErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
