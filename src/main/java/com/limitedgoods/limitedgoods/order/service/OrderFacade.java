package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final RedisStockService redisStockService;

    public OrderResponseDto redisCreateOrder(Long userId, OrderRequestDto orderRequestDto) {
        Long productId = orderRequestDto.getProductId();
        int quantity = orderRequestDto.getQuantity();

        redisStockService.decreaseStock(productId, quantity);

        try {
            return orderService.redisSaveOrder(userId, orderRequestDto); // 다른 빈 호출 → 프록시 정상 동작
        } catch (Exception e) {
            redisStockService.increaseStock(productId, quantity);
            throw e;
        }
    }

}
