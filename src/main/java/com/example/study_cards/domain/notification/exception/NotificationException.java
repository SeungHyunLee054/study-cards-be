package com.example.study_cards.domain.notification.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotificationException extends BaseException {

    private final NotificationErrorCode errorCode;

}
