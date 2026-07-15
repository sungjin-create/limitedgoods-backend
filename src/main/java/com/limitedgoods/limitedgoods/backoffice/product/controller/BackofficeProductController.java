package com.limitedgoods.limitedgoods.backoffice.product.controller;

import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.service.BackofficeProductService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/backoffice/product")
@RequiredArgsConstructor
public class BackofficeProductController {

    private final BackofficeProductService backofficeProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<BackofficeProductResponse>> getBackofficeProduct(){

        return ResponseEntity.ok(ApiResponse.success(backofficeProductService.getBackofficeProduct()));
    }

}
