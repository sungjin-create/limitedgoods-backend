package com.limitedgoods.limitedgoods.event.outbox.exception;

import java.util.UUID;

public class OutboxClaimOwnershipLostException
        extends RuntimeException {

    public OutboxClaimOwnershipLostException(
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