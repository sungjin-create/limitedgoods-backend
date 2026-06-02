package com.limitedgoods.limitedgoods.product.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> productRegister(@Valid @RequestBody ProductRegisterRequest productRegisterRequest) {
        ProductResponseDTO responseDTO = productService.registerProduct(productRegisterRequest);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse> productUpdate(@Valid @RequestBody ProductUpdateRequest productUpdateRequest) {

        ProductResponseDTO responseDTO = productService.updateProduct(productUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(responseDTO));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> productDelete(@RequestParam Long id) {
        if(id == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        productService.deleteProduct(id);

        return ResponseEntity.ok(ApiResponse.success(id));
    }

}
