package com.limitedgoods.limitedgoods.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminOrderSearchCondition {

    private OrderStatus status;
    private Long userId;
    private LocalDate from;
    private LocalDate to;
}