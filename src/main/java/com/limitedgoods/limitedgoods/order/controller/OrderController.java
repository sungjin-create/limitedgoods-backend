package com.limitedgoods.limitedgoods.order.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.order.application.create.CreateOrderUseCase;
import com.limitedgoods.limitedgoods.order.application.payment.PayOrderUseCase;
import com.limitedgoods.limitedgoods.order.application.query.OrderQueryService;
import com.limitedgoods.limitedgoods.order.dto.request.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderSummaryResponseDto;
import com.limitedgoods.limitedgoods.payment.dto.PaymentRequestDto;
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

    private final OrderQueryService orderQueryService;
    private final CreateOrderUseCase createOrderUseCase;
    private final PayOrderUseCase payOrderUseCase;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto orderRequestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        OrderResponseDto response = createOrderUseCase.execute(customUserDetails.getUserId(), orderRequestDto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<OrderResponseDto>> payOrder(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDto paymentRequestDto,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        OrderResponseDto response = payOrderUseCase.execute(
                customUserDetails.getUserId(),
                orderId,
                paymentRequestDto,
                idempotencyKey
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderSummaryResponseDto>>> findMyOrders(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        List<OrderSummaryResponseDto> response = orderQueryService.findMyOrders(
                customUserDetails.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> findOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        OrderDetailResponseDto response = orderQueryService.findOrderDetail(
                customUserDetails.getUserId(),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

//    @PostMapping("/{orderId}/cancel")
//    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
//            @PathVariable Long orderId,
//            @AuthenticationPrincipal CustomUserDetails customUserDetails
//    ) {
//        OrderResponseDto response = orderFacade.cancelOrder(
//                customUserDetails.getUserId(),
//                orderId
//        );
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }

//    @PostMapping("/{orderId}/refund/retry")
//    public ResponseEntity<ApiResponse<OrderResponseDto>> retryRefund(
//            @PathVariable Long orderId,
//            @AuthenticationPrincipal CustomUserDetails customUserDetails
//    ) {
//        OrderResponseDto response = orderFacade.retryRefund(
//                customUserDetails.getUserId(),
//                orderId
//        );
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }

}
