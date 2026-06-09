package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.ReservationPayload;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.order.payment.service.PaymentFailedException;
import com.limitedgoods.limitedgoods.order.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final RedisStockService redisStockService;
    private final PaymentService paymentService;
    private final RedisReservationService redisReservationService;

    private final long EXPIRED_SECONDS = 300;

    public OrderResponseDto createOrder(Long userId, OrderRequestDto request) {
        Long productId = request.getProductId();
        int quantity = request.getQuantity();

        redisStockService.decreaseStock(productId, quantity);

        try {
            OrderResponseDto order = orderService.createOrder(userId, request, EXPIRED_SECONDS);
            redisReservationService.createReservation(order.getId(), productId, quantity, EXPIRED_SECONDS);
            return order;
        } catch (RuntimeException e) {
            redisStockService.increaseStock(productId, quantity);
            throw e;
        }
    }

    public OrderResponseDto payOrder(Long userId, Long orderId, PaymentRequestDto request) {
        ReservationPayload reservation = redisReservationService.getReservation(orderId);
        if(reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }

        OrderPaymentInfo paymentInfo = orderService.startPayment(userId, orderId);

        try {
            paymentService.pay(orderId, paymentInfo.totalPrice(), request);
            OrderResponseDto response = orderService.completePayment(
                    userId, orderId, paymentInfo.productId(), paymentInfo.quantity()
            );
            redisReservationService.deleteReservation(orderId);
            return response;
        } catch (PaymentFailedException e) {
            orderService.failPayment(userId, orderId, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

}
