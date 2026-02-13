package com.example.study_cards.domain.bookmark.exception;

import com.example.study_cards.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements ErrorCode {

    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "이미 북마크된 카드입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
