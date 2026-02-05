package com.example.study_cards.domain.usercard.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserCardException extends BaseException {

    private final UserCardErrorCode errorCode;

}
