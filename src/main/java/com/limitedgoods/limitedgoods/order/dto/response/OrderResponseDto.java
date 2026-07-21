package com.limitedgoods.limitedgoods.order.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrderResponseDto (
        Long id,

        Long userId,

        long totalPrice,

        String status,

        LocalDateTime createdAt,

        LocalDateTime expiresAt
){

}
