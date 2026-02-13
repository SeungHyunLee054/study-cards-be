package com.example.study_cards.domain.bookmark.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookmarkException extends BaseException {

    private final BookmarkErrorCode errorCode;
}
