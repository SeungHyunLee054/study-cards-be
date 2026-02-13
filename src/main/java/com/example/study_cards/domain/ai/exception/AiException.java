package com.example.study_cards.domain.ai.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AiException extends BaseException {

    private final AiErrorCode errorCode;
}
