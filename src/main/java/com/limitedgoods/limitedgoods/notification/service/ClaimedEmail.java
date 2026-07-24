package com.limitedgoods.limitedgoods.notification.service;

import java.util.UUID;

public record ClaimedEmail(
        Long deliveryId,
        UUID claimToken
) {
}
