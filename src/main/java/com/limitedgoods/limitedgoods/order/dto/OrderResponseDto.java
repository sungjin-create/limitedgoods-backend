package com.limitedgoods.limitedgoods.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrderResponseDto {

    private Long id;

    private Long userId;

    private long totalPrice;

    private String status;

    private LocalDateTime createdAt;
}
