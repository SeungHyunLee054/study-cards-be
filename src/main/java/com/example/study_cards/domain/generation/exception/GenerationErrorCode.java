package com.example.study_cards.domain.generation.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GenerationErrorCode implements ErrorCode {

    GENERATED_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "생성된 카드를 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 변경입니다."),
    ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "이미 승인된 카드입니다."),
    ALREADY_REJECTED(HttpStatus.BAD_REQUEST, "이미 거부된 카드입니다."),
    ALREADY_MIGRATED(HttpStatus.BAD_REQUEST, "이미 이동된 카드입니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 문제 생성에 실패했습니다."),
    AI_NOT_ENABLED(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 비활성화되어 있습니다."),
    INVALID_AI_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 파싱할 수 없습니다."),
    INVALID_SOURCE_CARD_SELECTION(HttpStatus.BAD_REQUEST, "선택한 원본 카드가 유효하지 않습니다."),
    NO_CARDS_TO_GENERATE(HttpStatus.BAD_REQUEST, "생성할 카드가 없습니다.");

    private final HttpStatus status;
    private final String message;
}
