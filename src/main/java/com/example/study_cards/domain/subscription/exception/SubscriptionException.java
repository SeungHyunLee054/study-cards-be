package com.example.study_cards.domain.subscription.exception;

import com.example.study_cards.common.exception.BaseException;
import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class SubscriptionException extends BaseException {

    private final SubscriptionErrorCode errorCode;

    public SubscriptionException(SubscriptionErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
