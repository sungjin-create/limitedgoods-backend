package com.limitedgoods.limitedgoods.backoffice.dashboard.controller;

import com.limitedgoods.limitedgoods.backoffice.dashboard.dto.BackofficeResponse;
import com.limitedgoods.limitedgoods.backoffice.dashboard.service.BackofficeDashboardService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/backoffice/dashboard")
@RequiredArgsConstructor
public class BackofficeDashboardController {

    private final BackofficeDashboardService backofficeDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<BackofficeResponse>> getAdminDashboard(){
        return ResponseEntity.ok(ApiResponse.success(backofficeDashboardService.getDashboard()));
    }
}
