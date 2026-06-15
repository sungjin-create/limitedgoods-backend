package com.limitedgoods.limitedgoods.order.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.AdminOrderSearchCondition;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminOrderResponseDto>>> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        AdminOrderSearchCondition condition = new AdminOrderSearchCondition();
        condition.setStatus(status);
        condition.setUserId(userId);
        condition.setFrom(from);
        condition.setTo(to);

        Page<AdminOrderResponseDto> response =
                adminOrderService.searchOrders(condition, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderResponseDto>> getOrderDetail(
            @PathVariable Long orderId
    ) {
        AdminOrderResponseDto response =
                adminOrderService.getOrderDetail(orderId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}