package com.example.study_cards.common.util;

import com.example.study_cards.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogUtils {

    private LogUtils() {
    }

    public static void logWarn(Throwable throwable) {
        if (throwable instanceof BaseException baseException) {
            log.warn("예외 발생: {} (ErrorCode: {})",
                    baseException.getErrorCode().getMessage(),
                    baseException.getErrorCode());
        } else {
            log.warn("예외 발생: {}", throwable.getMessage());
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > 0) {
            StackTraceElement first = stackTrace[0];
            log.warn("발생 위치: {}:{} - Thread: {}, Method: {}",
                    first.getClassName(), first.getLineNumber(),
                    Thread.currentThread().getName(), first.getMethodName());
        }
    }

    public static void logError(Throwable throwable) {
        log.error("예외 발생: {}", throwable.getMessage());
        log.error("전체 예외 스택:", throwable);
    }
}
