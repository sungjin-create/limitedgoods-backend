package com.limitedgoods.limitedgoods.backoffice.order.dto;

public record OrderSummaryResponse (
    long paymentPendingCount,
    long paidCount,
    long cancelRequestedCount,
    long paymentFailedCount
){

}
