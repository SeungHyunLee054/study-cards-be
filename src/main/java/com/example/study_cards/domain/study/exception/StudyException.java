package com.example.study_cards.domain.study.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StudyException extends BaseException {

    private final StudyErrorCode errorCode;

}
