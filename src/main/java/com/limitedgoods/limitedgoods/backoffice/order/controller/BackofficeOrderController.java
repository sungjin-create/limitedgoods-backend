package com.limitedgoods.limitedgoods.backoffice.order.controller;

import com.limitedgoods.limitedgoods.backoffice.order.dto.OrdersResponse;
import com.limitedgoods.limitedgoods.backoffice.order.service.BackofficeOrderService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/backoffice/order")
@RequiredArgsConstructor
public class BackofficeOrderController {

    private final BackofficeOrderService backofficeOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<OrdersResponse>> findBackofficeOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ){
        return ResponseEntity.ok(ApiResponse.success(backofficeOrderService.findBackofficeOrders(startAt, endAt)));
    }

}
