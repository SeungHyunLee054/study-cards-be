package com.example.study_cards.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse(int status, String message, LocalDateTime timestamp, List<FieldError> errors) {

    public static CommonResponse of(int status, String message) {
        return CommonResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static CommonResponse of(int status, String message, BindingResult bindingResult) {
        return CommonResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(FieldError.of(bindingResult))
                .build();
    }

    public record FieldError(String field, String rejectedValue, String reason) {

        public static List<FieldError> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors()
                    .stream()
                    .map(error -> new FieldError(
                            error.getField(),
                            error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                            error.getDefaultMessage()))
                    .toList();
        }
    }
}
