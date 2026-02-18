package com.example.study_cards.domain.ai.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_FEATURE_NOT_AVAILABLE(HttpStatus.FORBIDDEN, "AI 기능을 사용할 수 없는 플랜입니다."),
    GENERATION_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 생성 횟수 제한을 초과했습니다."),
    AI_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 카드 생성에 실패했습니다."),
    INVALID_AI_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 파싱할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_TEXT_EXTRACTION_FAILED(HttpStatus.BAD_REQUEST, "파일에서 텍스트를 추출할 수 없습니다."),
    EMPTY_EXTRACTED_TEXT(HttpStatus.BAD_REQUEST, "파일에서 추출된 텍스트가 비어 있습니다.");

    private final HttpStatus status;
    private final String message;
}
