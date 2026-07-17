package com.limitedgoods.limitedgoods.backoffice.product.service;

import com.limitedgoods.limitedgoods.backoffice.product.dto.*;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.entity.ProductType;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.limitedgoods.limitedgoods.product.entity.ProductStatus.*;

@Service
@RequiredArgsConstructor
public class BackofficeProductService {

    private final ProductRepository productRepository;

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    DRAFT, EnumSet.of(PREPARING, SCHEDULED, ACTIVE),
                    PREPARING, EnumSet.of(SCHEDULED, ACTIVE, PAUSED, HIDDEN, ARCHIVED),
                    SCHEDULED, EnumSet.of(ACTIVE, PAUSED, HIDDEN, ARCHIVED),
                    ACTIVE, EnumSet.of(PAUSED, HIDDEN, ARCHIVED),
                    PAUSED, EnumSet.of(PREPARING, SCHEDULED, ACTIVE, ARCHIVED),
                    HIDDEN, EnumSet.of(PREPARING, SCHEDULED, ACTIVE, ARCHIVED),
                    ARCHIVED, EnumSet.noneOf(ProductStatus.class)
            );

    public ProductListResponse getBackofficeProduct() {

        ProductSummaryResponse productSummary =
                productRepository.getBackofficeProductSummary();


        List<ProductResponse> productList =
                productRepository.findProductList();

        return ProductListResponse.builder()
                .summary(productSummary)
                .products(productList)
                .build();
    }

    @Transactional
    public ProductResponse registerProduct (ProductRegisterRequest productRegisterRequest) {
        String name = productRegisterRequest.getName();
        String description = productRegisterRequest.getDescription();
        int price = productRegisterRequest.getPrice();
        int initialStock = productRegisterRequest.getInitialStock();
        int soldCount = 0;
        Integer maxPurchaseQuantity = productRegisterRequest.getMaxPurchaseQuantity();
        ProductType type = productRegisterRequest.getType();
        ProductStatus status = productRegisterRequest.getStatus();
        LocalDateTime saleStartAt = productRegisterRequest.getSaleStartAt();
        LocalDateTime saleEndAt = productRegisterRequest.getSaleEndAt();

        //판매 시작, 판매 끝 값 검사
        checkStatusWithSaleTime(status, saleStartAt, saleEndAt);

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setInitialStock(initialStock);
        product.setStock(initialStock);
        product.setSoldCount(soldCount);
        product.setMaxPurchaseQuantity(maxPurchaseQuantity);
        product.setType(type);
        product.setStatus(status);
        product.setSaleStartAt(saleStartAt);
        product.setSaleEndAt(saleEndAt);

        Product saveProduct = productRepository.save(product);

        return toResponse(saveProduct);
    }

    @Transactional
    public ProductResponse updateProduct (ProductUpdateRequest productUpdateRequest) {
        Long id = productUpdateRequest.getId();
        String nextName = productUpdateRequest.getName();
        String nextDescription = productUpdateRequest.getDescription();
        int nextPrice = productUpdateRequest.getPrice();
        Integer nextMaxPurchaseQuantity = productUpdateRequest.getMaxPurchaseQuantity();
        ProductType nextType = productUpdateRequest.getType();
        ProductStatus nextStatus = productUpdateRequest.getStatus();
        LocalDateTime nextSaleStartAt = productUpdateRequest.getSaleStartAt();
        LocalDateTime nextSaleEndAt = productUpdateRequest.getSaleEndAt();

        Product currentProduct = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        validateStatusTransition(currentProduct.getStatus(), nextStatus);

        validateSaleScheduleForStatus(nextStatus, nextSaleStartAt, nextSaleEndAt);

        currentProduct.setName(nextName);
        currentProduct.setDescription(nextDescription);
        currentProduct.setPrice(nextPrice);
        currentProduct.setMaxPurchaseQuantity(nextMaxPurchaseQuantity);
        currentProduct.setType(nextType);
        currentProduct.setStatus(nextStatus);
        currentProduct.setSaleStartAt(nextSaleStartAt);
        currentProduct.setSaleEndAt(nextSaleEndAt);


        return toResponse(currentProduct);
    }

    @Transactional
    public void deleteProduct (Long id) {
        productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        productRepository.deleteById(id);

    }

    private void checkStatusWithSaleTime(ProductStatus status, LocalDateTime saleStartAt, LocalDateTime saleEndAt) {
        //판매 예정인 경우는 시간입력 필수
        if(status.equals(ProductStatus.SCHEDULED)) {
            if(saleStartAt == null || saleEndAt == null) {
                throw new BusinessException(ErrorCode.HAS_NO_SALE_TIME);
            }
        }

        //시작시간, 종료시간 중 하나만 입력되는 경우 예외처리
        if ((saleStartAt == null) != (saleEndAt == null)) {
            throw new BusinessException(ErrorCode.HAS_NO_SALE_TIME);
        }

        //시작시간 <= 종료시간이 아닌 경우 예외처리
        if(saleStartAt != null && !saleStartAt.isBefore(saleEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_TIME);
        }
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .maxPurchaseQuantity(product.getMaxPurchaseQuantity())
                .type(product.getType())
                .status(product.getStatus())
                .saleStartAt(product.getSaleStartAt())
                .saleEndAt(product.getSaleEndAt())
                .build();
    }

    private void validateStatusTransition(
            ProductStatus currentStatus,
            ProductStatus nextStatus
    ) {
        if (currentStatus == nextStatus) {
            return; // 일정만 수정하는 경우
        }

        Set<ProductStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);

        if (allowed == null || !allowed.contains(nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS_TRANSITION);
        }
    }

    private void validateSaleScheduleForStatus(
            ProductStatus nextStatus,
            LocalDateTime saleStartAt,
            LocalDateTime saleEndAt
    ) {
        if (saleStartAt != null && saleEndAt != null
                && !saleStartAt.isBefore(saleEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_TIME);
        }

        if (nextStatus == ProductStatus.SCHEDULED) {
            if (saleStartAt == null || saleEndAt == null) {
                throw new BusinessException(ErrorCode.HAS_NO_SALE_TIME);
            }

            if (!saleStartAt.isAfter(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.SALE_START_MUST_BE_FUTURE);
            }
        }

        if (nextStatus == ProductStatus.ACTIVE
                && saleEndAt != null
                && !saleEndAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SALE_ALREADY_ENDED);
        }
    }

}
