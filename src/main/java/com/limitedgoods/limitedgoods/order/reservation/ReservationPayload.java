package com.limitedgoods.limitedgoods.order.reservation;

import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationPayload {
    private Long orderId;
    private List<OrderItemsListDto> items;
}
