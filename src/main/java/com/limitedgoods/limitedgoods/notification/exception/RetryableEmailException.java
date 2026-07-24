package com.limitedgoods.limitedgoods.notification.exception;

public class RetryableEmailException
        extends RuntimeException {

    public RetryableEmailException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
