package com.example.study_cards.domain.category.exception;

import com.example.study_cards.common.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CategoryException extends BaseException {

    private final CategoryErrorCode errorCode;

}
