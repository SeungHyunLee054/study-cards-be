package com.example.study_cards.domain.generation.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenerationException extends BaseException {

    private final GenerationErrorCode errorCode;

}
