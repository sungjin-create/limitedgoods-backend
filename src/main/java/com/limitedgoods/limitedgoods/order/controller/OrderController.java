package com.limitedgoods.limitedgoods.order.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.order.dto.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.order.service.OrderFacade;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        OrderResponseDto response = orderFacade.payOrder(
                customUserDetails.getUserId(),
                orderId,
                paymentRequestDto,
                idempotencyKey
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDetailResponseDto>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<OrderDetailResponseDto> response = orderService.getMyOrders(
                customUserDetails.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        OrderDetailResponseDto response = orderService.getOrderDetail(
                customUserDetails.getUserId(),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
