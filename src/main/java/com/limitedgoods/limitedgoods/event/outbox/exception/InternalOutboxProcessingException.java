package com.limitedgoods.limitedgoods.event.outbox.exception;

public class InternalOutboxProcessingException
        extends RuntimeException {

    public InternalOutboxProcessingException(
            Long eventId
    ) {
        super(
                "One or more internal consumers failed. eventId="
                        + eventId
        );
    }
}