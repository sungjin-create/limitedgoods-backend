package com.limitedgoods.limitedgoods.notification.exception;

public class EmailInfrastructureException
        extends RuntimeException {

    public EmailInfrastructureException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}