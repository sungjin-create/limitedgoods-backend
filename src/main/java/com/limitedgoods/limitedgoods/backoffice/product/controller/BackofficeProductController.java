package com.limitedgoods.limitedgoods.backoffice.product.controller;

import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductListResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.backoffice.product.service.BackofficeProductService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/backoffice/product")
@RequiredArgsConstructor
public class BackofficeProductController {

    private final BackofficeProductService backofficeProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getBackofficeProductList(){

        return ResponseEntity.ok(ApiResponse.success(backofficeProductService.getBackofficeProduct()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ProductResponse>> productRegister(
            @Valid @RequestBody ProductRegisterRequest productRegisterRequest) {
        ProductResponse response = backofficeProductService.registerProduct(productRegisterRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<ProductResponse>> productUpdate(
            @Valid @RequestBody ProductUpdateRequest productUpdateRequest) {
        ProductResponse response = backofficeProductService.updateProduct(productUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> productDelete(@RequestParam Long id) {
        if(id == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        backofficeProductService.deleteProduct(id);

        return ResponseEntity.ok(ApiResponse.success(id));
    }

}
