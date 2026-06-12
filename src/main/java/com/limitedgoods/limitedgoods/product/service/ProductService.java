package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.stock.service.RedisStockService;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisStockService redisStockService;

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

        // DB 저장 후 Redis 재고 초기화
        redisStockService.initStock(saveProduct.getId(), saveProduct.getStock());

        return ProductResponseDTO.builder().id(saveProduct.getId()).name(saveProduct.getName()).build();
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

        // 재고 변경 시 Redis도 동기화
        redisStockService.initStock(updateProduct.getId(), updateProduct.getStock());

        return  ProductResponseDTO.builder().id(id).name(updateProduct.getName()).build();
    }

    @Transactional
    public void deleteProduct (Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        productRepository.deleteById(id);

    }
    public void initRedisStock() {
        List<Product> products = productRepository.findAll();
        products.forEach(p -> redisStockService.initStock(p.getId(), p.getStock()));
    }
}
