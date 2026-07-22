package com.limitedgoods.limitedgoods.backoffice.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OrdersResponse {

    private OrderSummaryResponse summary;
    private List<OrderResponse> orders;

}
