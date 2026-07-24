package com.limitedgoods.limitedgoods.notification.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.common.response.PageResponse;
import com.limitedgoods.limitedgoods.notification.dto.AdminEmailDeliveryResponse;
import com.limitedgoods.limitedgoods.notification.service.AdminEmailDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/email-deliveries")
@RequiredArgsConstructor
public class AdminEmailDeliveryController {

    private final AdminEmailDeliveryService service;

    @GetMapping("/dead")
    public ResponseEntity<ApiResponse<PageResponse<AdminEmailDeliveryResponse>>> findDead(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.findDead(pageable)));
    }

    @PostMapping("/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(
            @PathVariable Long deliveryId
    ) {
        service.requeue(deliveryId);

        return ResponseEntity.ok(ApiResponse.success());
    }
}