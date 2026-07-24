package com.limitedgoods.limitedgoods.event.outbox.exception;

import java.util.UUID;

public class OutboxEventClaimOwnershipLostException
        extends RuntimeException {

    public OutboxEventClaimOwnershipLostException(
            Long eventId,
            UUID claimToken
    ) {
        super(
                "Outbox claim ownership was lost. eventId="
                        + eventId
                        + ", claimToken="
                        + claimToken
        );
    }
}