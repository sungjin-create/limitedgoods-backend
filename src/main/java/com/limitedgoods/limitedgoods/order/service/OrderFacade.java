//package com.limitedgoods.limitedgoods.order.service;
//
//import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
//import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.orm.ObjectOptimisticLockingFailureException;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.retry.annotation.Recover;
//import org.springframework.retry.annotation.Retryable;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class OrderFacade {
//
//    private final OrderService orderService;
//
//    @Retryable(
//            retryFor = ObjectOptimisticLockingFailureException.class,
//            maxAttempts = 10,
//            backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 2000, random = true)
//    )
//    public OrderResponseDto createOrder(
//            Long userId,
//            OrderRequestDto dto) {
//        return orderService.createOrder(userId, dto);
//    }
//
//    @Recover
//    public OrderResponseDto recover(
//            ObjectOptimisticLockingFailureException e,
//            Long userId,
//            OrderRequestDto dto) {
//        throw new RuntimeException("재시도 실패", e);
//    }
//}