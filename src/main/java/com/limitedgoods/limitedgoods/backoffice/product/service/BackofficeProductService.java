package com.limitedgoods.limitedgoods.backoffice.product.service;

import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductSummaryResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.BackofficeProductsResponse;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.product.dto.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.dto.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductType;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public ProductResponseDTO registerProduct (ProductRegisterRequest productRegisterRequest) {
        String name = productRegisterRequest.getName();
        String description = productRegisterRequest.getDescription();
        int price = productRegisterRequest.getPrice();
        int stock = productRegisterRequest.getStock();
        ProductType type = productRegisterRequest.getType();
        boolean visible = productRegisterRequest.isVisible();
        LocalDateTime saleStartAt = productRegisterRequest.getSaleStartAt();
        LocalDateTime saleEndAt = productRegisterRequest.getSaleEndAt();
        Integer maxPurchaseQuantity = productRegisterRequest.getMaxPurchaseQuantity();

        if(saleStartAt == null || saleEndAt == null
                || saleStartAt.isAfter(saleEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_TIME);
        }

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setType(type);
        product.setVisible(visible);
        product.setSaleStartAt(saleStartAt);
        product.setSaleEndAt(saleEndAt);
        product.setMaxPurchaseQuantity(maxPurchaseQuantity);

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

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .maxPurchaseQuantity(product.getMaxPurchaseQuantity())
                .type(product.getType())
                .visible(product.isVisible())
                .saleStartAt(product.getSaleStartAt())
                .saleEndAt(product.getSaleEndAt())
                .build();
    }
}
