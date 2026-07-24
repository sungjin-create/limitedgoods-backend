package com.limitedgoods.limitedgoods.notification.exception;

import java.util.UUID;

public class ClaimOwnershipLostException extends RuntimeException {

    public ClaimOwnershipLostException(
            Long deliveryId,
            UUID claimToken
    ) {
        super(
                "Email delivery claim ownership was lost. deliveryId="
                        + deliveryId
                        + ", claimToken="
                        + claimToken
        );
    }
}
