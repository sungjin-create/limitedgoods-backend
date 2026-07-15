package com.limitedgoods.limitedgoods.backoffice.monitoring.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.backoffice.monitoring.dto.BackofficeBusinessMetricResponse;
import com.limitedgoods.limitedgoods.backoffice.monitoring.service.BackofficeMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/backoffice/monitoring")
@RequiredArgsConstructor
public class BackofficeMonitoringController {

    private final BackofficeMonitoringService backofficeMonitoringService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        return ResponseEntity.ok(
                ApiResponse.success(backofficeMonitoringService.getOverview())
        );
    }

    @GetMapping("/business")
    public ResponseEntity<ApiResponse<BackofficeBusinessMetricResponse>> getBusinessMetrics() {
        return ResponseEntity.ok(ApiResponse.success(
                backofficeMonitoringService.getBusinessMetrics()
        ));
    }

}