package com.limitedgoods.limitedgoods.product.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import com.limitedgoods.limitedgoods.product.dto.ProductResponseDTO;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.limitedgoods.limitedgoods.product.entity.ProductStatus.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProducts(Pageable pageable) {
        return productRepository.findProductByStatusIn(PREPARING, SCHEDULED, ACTIVE, PAUSED, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProduct(Pageable pageable, String keyword) {
        return productRepository.searchByKeyword(pageable, keyword).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public void validatePurchasableProducts(List<OrderItemsListDto> items) {
        Set<Long> productIds = items.stream()
                .map(OrderItemsListDto::getProductId)
                .collect(Collectors.toSet());

        List<Product> products =
                productRepository.findAllById(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()
                ));

        for (OrderItemsListDto item : items) {
            Product product =
                    productMap.get(item.getProductId());

            if (product == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_PRODUCT_ID
                );
            }

            if(!product.isPurchasableAt(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_SALE_STATUS);
            }
        }
    }

    private void validatePurchasable(Product product, LocalDateTime now) {
        boolean statusAllowsSale =
                product.getStatus() == ProductStatus.ACTIVE
                        || (
                        product.getStatus() == ProductStatus.SCHEDULED
                                && product.getSaleStartAt() != null
                                && !product.getSaleStartAt().isAfter(now)
                );

        boolean saleStarted =
                product.getSaleStartAt() == null ||
                        !product.getSaleStartAt().isAfter(now);

        boolean saleNotEnded =
                product.getSaleEndAt() == null ||
                        now.isBefore(product.getSaleEndAt());

        if (!statusAllowsSale || !saleStarted || !saleNotEnded) {
            throw new BusinessException(
                    ErrorCode.INVALID_PRODUCT_SALE_STATUS
            );
        }
    }

    private ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .type(product.getType())
                .status(product.getStatus())
                .saleStartAt(product.getSaleStartAt())
                .saleEndAt(product.getSaleEndAt())
                .build();
    }

}
