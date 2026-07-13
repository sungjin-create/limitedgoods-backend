package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
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

        return toResponse(saveProduct);
    }

    @Transactional
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

        return toResponse(updateProduct);
    }

    @Transactional
    public void deleteProduct (Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        productRepository.deleteById(id);

    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProduct(Pageable pageable, String keyword) {
        return productRepository.searchByKeyword(pageable, keyword).map(this::toResponse);
    }

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
