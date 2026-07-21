package com.limitedgoods.limitedgoods.order.application.create;

public record OrderAdmissionClaim(
        String admissionToken,
        Long userId,
        Long productId,
        String claimId
) {
}
