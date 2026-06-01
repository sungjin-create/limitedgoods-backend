package com.limitedgoods.limitedgoods.product.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody ProductRegisterRequest productRegisterRequest) {
        productService.registerProduct(productRegisterRequest);
        return ResponseEntity.ok(ApiResponse.success("success"));
    }

}
