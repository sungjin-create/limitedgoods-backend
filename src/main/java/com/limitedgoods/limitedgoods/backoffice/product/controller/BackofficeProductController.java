package com.limitedgoods.limitedgoods.backoffice.product.controller;

import com.limitedgoods.limitedgoods.backoffice.product.dto.*;
import com.limitedgoods.limitedgoods.backoffice.product.service.BackofficeProductService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/backoffice/product")
@RequiredArgsConstructor
public class BackofficeProductController {

    private final BackofficeProductService backofficeProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getBackofficeProductList(
            @RequestParam(required = false) ProductStatus status) {
        return ResponseEntity.ok(ApiResponse.success(backofficeProductService.findBackofficeProductList(status)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ProductResponse>> productRegister(
            @Valid @RequestBody ProductRegisterRequest productRegisterRequest) {
        ProductResponse response = backofficeProductService.registerProduct(productRegisterRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<ProductResponse>> productUpdate(
            @Valid @RequestBody ProductUpdateRequest productUpdateRequest) {
        ProductResponse response = backofficeProductService.updateProduct(productUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(backofficeProductService.adjustStock(request)));
    }

    @GetMapping("/{productId}/stock-overview")
    public ResponseEntity<ApiResponse<ProductStockOverViewResponse>> getProductStockOverView(
            @PathVariable Long productId) {
        ProductStockOverViewResponse response = backofficeProductService.findProductStockOverView(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
