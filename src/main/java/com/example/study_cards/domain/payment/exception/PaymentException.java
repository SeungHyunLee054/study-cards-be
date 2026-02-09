package com.example.study_cards.domain.payment.exception;

import com.example.study_cards.common.exception.BaseException;
import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentException extends BaseException {

    private final PaymentErrorCode errorCode;

    public PaymentException(PaymentErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
