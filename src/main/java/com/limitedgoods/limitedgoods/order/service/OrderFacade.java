package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final RedisLockService redisLockService;

    public OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequestDto) {
        return orderService.createOrder(userId, orderRequestDto);
    }

}
