package com.example.study_cards.domain.payment.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 결제입니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 결제입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_CUSTOMER_KEY_MISMATCH(HttpStatus.BAD_REQUEST, "고객 키가 일치하지 않습니다."),
    PAYMENT_NOT_SUPPORTED_FOR_CYCLE(HttpStatus.BAD_REQUEST, "연간 구독만 일반 결제 확인을 지원합니다."),
    PAYMENT_CONFIRMATION_FAILED(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "결제 취소에 실패했습니다."),

    BILLING_KEY_ISSUE_FAILED(HttpStatus.BAD_REQUEST, "빌링키 발급에 실패했습니다."),
    BILLING_PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "자동 결제에 실패했습니다."),
    BILLING_NOT_SUPPORTED_FOR_CYCLE(HttpStatus.BAD_REQUEST, "월간 구독만 빌링 결제를 지원합니다."),

    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "유효하지 않은 웹훅 서명입니다.");

    private final HttpStatus status;
    private final String message;
}
