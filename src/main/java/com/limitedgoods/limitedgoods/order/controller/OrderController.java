package com.limitedgoods.limitedgoods.order.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.service.OrderFacade;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor

public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto orderRequestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        OrderResponseDto response = orderFacade.createOrder(
                customUserDetails.getUserId(),
                orderRequestDto
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<OrderResponseDto>> payOrder(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDto paymentRequestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        OrderResponseDto response = orderFacade.payOrder(
                customUserDetails.getUserId(),
                orderId,
                paymentRequestDto
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/create/pessimistic")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrderPessimistic(
            @Valid @RequestBody OrderRequestDto orderRequestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();
        OrderResponseDto response = orderService.saveOrderWithPessimisticLock(userId, orderRequestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
