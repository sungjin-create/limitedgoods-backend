package com.limitedgoods.limitedgoods.order.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor

public class OrderController {

    private final OrderService orderService;


    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto,
                                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();

        OrderResponseDto orderResponseDto = orderService.createOrder(userId, orderRequestDto);

        return ResponseEntity.ok(ApiResponse.success(orderResponseDto));
    }
}
