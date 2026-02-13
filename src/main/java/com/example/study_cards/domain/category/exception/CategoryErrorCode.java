package com.example.study_cards.domain.category.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 카테고리 코드입니다."),
    CATEGORY_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "하위 카테고리가 존재하여 삭제할 수 없습니다."),
    INVALID_PARENT_CATEGORY(HttpStatus.BAD_REQUEST, "유효하지 않은 상위 카테고리입니다.");

    private final HttpStatus status;
    private final String message;
}
