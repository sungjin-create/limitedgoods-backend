package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductStatusService {

    private final ProductRepository productRepository;

    @Transactional
    public int activateScheduledProducts() {
        return productRepository.activateProductsReadyForSale(
                ProductStatus.ACTIVE,
                ProductStatus.SCHEDULED
        );
    }
}