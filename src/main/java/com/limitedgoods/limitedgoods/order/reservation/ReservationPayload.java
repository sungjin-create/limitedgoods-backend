package com.limitedgoods.limitedgoods.order.reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationPayload {
    private Long orderId;
    private Long productId;
    private int quantity;
}
