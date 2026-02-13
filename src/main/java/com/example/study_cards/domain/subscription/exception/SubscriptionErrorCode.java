package com.example.study_cards.domain.subscription.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 구독 중인 요금제가 있습니다."),
    SUBSCRIPTION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화된 구독이 아닙니다."),
    SUBSCRIPTION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 구독입니다."),
    SUBSCRIPTION_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 구독입니다."),
    INVALID_PLAN_CHANGE(HttpStatus.BAD_REQUEST, "유효하지 않은 요금제 변경입니다."),
    FREE_PLAN_NOT_PURCHASABLE(HttpStatus.BAD_REQUEST, "무료 요금제는 구매할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
