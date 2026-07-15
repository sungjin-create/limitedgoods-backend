package com.limitedgoods.limitedgoods.backoffice.product.service;

import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductSummaryResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductsResponse;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackofficeProductService {

    private final ProductRepository productRepository;

    public BackofficeProductResponse getBackofficeProduct() {

        BackofficeProductSummaryResponse productSummary =
                productRepository.getBackofficeProductSummary();


        List<BackofficeProductsResponse> productList =
                productRepository.findProductList();

        return BackofficeProductResponse.builder()
                .summary(productSummary)
                .products(productList)
                .build();
    }
}
