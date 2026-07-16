package com.limitedgoods.limitedgoods.backoffice.product.controller;

import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.service.BackofficeProductService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
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
    public ResponseEntity<ApiResponse<BackofficeProductResponse>> getBackofficeProduct(){

        return ResponseEntity.ok(ApiResponse.success(backofficeProductService.getBackofficeProduct()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> productRegister(
            @Valid @RequestBody ProductRegisterRequest productRegisterRequest) {
        ProductResponseDTO responseDTO = backofficeProductService.registerProduct(productRegisterRequest);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse> productUpdate(@Valid @RequestBody ProductUpdateRequest productUpdateRequest) {
        ProductResponseDTO responseDTO = backofficeProductService.updateProduct(productUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
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
