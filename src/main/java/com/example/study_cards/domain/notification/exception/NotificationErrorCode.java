package com.example.study_cards.domain.notification.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "푸시 알림 발송에 실패했습니다."),
    INVALID_FCM_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 FCM 토큰입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "알림에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
