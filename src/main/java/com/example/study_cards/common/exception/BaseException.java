package com.example.study_cards.common.exception;

public abstract class BaseException extends RuntimeException {

    public abstract ErrorCode getErrorCode();
}
