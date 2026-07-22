package com.limitedgoods.limitedgoods.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(
            ErrorCode errorCode,
            String additionalMessage
    ) {
        super(createMessage(errorCode, additionalMessage));
        this.errorCode = errorCode;
    }

    private static String createMessage(
            ErrorCode errorCode,
            String additionalMessage
    ) {
        if (additionalMessage == null || additionalMessage.isBlank()) {
            return errorCode.getMessage();
        }

        return errorCode.getMessage() + " " + additionalMessage;
    }
}