package com.limitedgoods.limitedgoods.notification.exception;

import java.util.UUID;

public class EmailDeliveryClaimOwnershipLostException extends RuntimeException {

    public EmailDeliveryClaimOwnershipLostException(
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
