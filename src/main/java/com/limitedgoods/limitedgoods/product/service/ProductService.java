package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public void registerProduct (ProductRegisterRequest productRegisterRequest) {
        String name = productRegisterRequest.getName();
        String description = productRegisterRequest.getDescription();
        int price = productRegisterRequest.getPrice();
        int stock = productRegisterRequest.getStock();

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);

        productRepository.save(product);

    }
}
