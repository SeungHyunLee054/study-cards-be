package com.example.study_cards.domain.user.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserException extends BaseException {

    private final UserErrorCode errorCode;

}
