package com.limitedgoods.limitedgoods.notification.service;

import java.util.UUID;

public record ClaimedEmailDelivery(
        Long deliveryId,
        UUID claimToken
) {
}
