package com.limitedgoods.limitedgoods.event.outbox.exception;

public class InternalEventProcessingException
        extends RuntimeException {

    public InternalEventProcessingException(
            Long eventId
    ) {
        super(
                "One or more internal consumers failed. eventId="
                        + eventId
        );
    }
}