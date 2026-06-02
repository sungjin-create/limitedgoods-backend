package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponseDTO registerProduct (ProductRegisterRequest productRegisterRequest) {
        String name = productRegisterRequest.getName();
        String description = productRegisterRequest.getDescription();
        int price = productRegisterRequest.getPrice();
        int stock = productRegisterRequest.getStock();

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);

        Product saveProduct = productRepository.save(product);
        return ProductResponseDTO.builder().id(saveProduct.getId()).name(saveProduct.getName()).build();
    }

    public ProductResponseDTO updateProduct (ProductUpdateRequest productUpdateRequest) {
        Long id = productUpdateRequest.getId();
        String name = productUpdateRequest.getName();
        String description = productUpdateRequest.getDescription();
        int price = productUpdateRequest.getPrice();
        int stock = productUpdateRequest.getStock();

        Product updateProduct = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        updateProduct.setName(name);
        updateProduct.setDescription(description);
        updateProduct.setPrice(price);
        updateProduct.setStock(stock);

        productRepository.save(updateProduct);

        return  ProductResponseDTO.builder().id(id).name(updateProduct.getName()).build();
    }

    public void deleteProduct (Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        productRepository.deleteById(id);

    }

}
