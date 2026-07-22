package com.limitedgoods.limitedgoods.backoffice.product.service;

import com.limitedgoods.limitedgoods.backoffice.product.dto.request.ProductRegisterRequest;
import com.limitedgoods.limitedgoods.backoffice.product.dto.request.ProductUpdateRequest;
import com.limitedgoods.limitedgoods.backoffice.product.dto.request.StockAdjustmentRequest;
import com.limitedgoods.limitedgoods.backoffice.product.dto.response.ProductListResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.response.ProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.response.ProductStockOverViewResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.response.ProductSummaryResponse;
import com.limitedgoods.limitedgoods.backoffice.product.query.BackofficeProductQueryRepository;
import com.limitedgoods.limitedgoods.backoffice.product.query.ProductOrderSummaryQueryResult;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.policy.ProductOrderPolicy;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.entity.ProductType;
import com.limitedgoods.limitedgoods.product.policy.ProductStatusPolicy;
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
    private final BackofficeProductQueryRepository backofficeProductQueryRepository;
    private final ProductStatusPolicy productStatusPolicy;

    @Transactional
    public ProductListResponse findBackofficeProductList(ProductStatus status) {
        ProductSummaryResponse productSummary =
                backofficeProductQueryRepository.findProductSummary();

        List<ProductResponse> productList;
        if(status == null) {
            productList = backofficeProductQueryRepository.findAllProducts();
        } else {
            productList = backofficeProductQueryRepository.findAllProductsByStatus(status);
        }

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

        //상품 등록시 가능한 상태검사
        productStatusPolicy.validateRegisterStatus(status);
        //판매 시작, 판매 끝 값 검사
        productStatusPolicy.validateSaleSchedule(status, saleStartAt, saleEndAt);

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
        product.setUpdatedAt(LocalDateTime.now());

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

        Product currentProduct = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        //상태변경 검사
        productStatusPolicy.validateTransition(currentProduct.getStatus(), nextStatus);

        //상태에 따른 일정 변경 검사
        productStatusPolicy.validateSaleSchedule(nextStatus, nextSaleStartAt, nextSaleEndAt);

        currentProduct.setName(nextName);
        currentProduct.setDescription(nextDescription);
        currentProduct.setPrice(nextPrice);
        currentProduct.setMaxPurchaseQuantity(nextMaxPurchaseQuantity);
        currentProduct.setType(nextType);
        currentProduct.setStatus(nextStatus);
        currentProduct.setSaleStartAt(nextSaleStartAt);
        currentProduct.setSaleEndAt(nextSaleEndAt);
        currentProduct.setUpdatedAt(LocalDateTime.now());

        return toResponse(currentProduct);
    }

    @Transactional
    public ProductResponse adjustStock(StockAdjustmentRequest request) {
        Product product = productRepository.findByIdWithLock(request.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        int quantity = request.getQuantity();
        int currentStock = product.getStock();
        int adjustedStock;

        ProductStatus status = product.getStatus();

        if(status == ACTIVE || status == ARCHIVED) {
            throw new BusinessException(ErrorCode.STOCK_ADJUSTMENT_NOT_ALLOWED_STATUS, "현재 STATUS = " + status);
        }

        switch (request.getAdjustmentType()) {
            case INCREASE -> {
                if (quantity == 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
                long increasedStock = (long) currentStock + quantity;
                if (increasedStock > Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_INPUT);
                adjustedStock = (int) increasedStock;
            }
            case DECREASE -> {
                if (quantity == 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
                if (currentStock < quantity) throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
                adjustedStock = currentStock - quantity;
            }
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        product.setStock(adjustedStock);
        product.setUpdatedAt(LocalDateTime.now());
        return toResponse(product);
    }

    @Transactional
    public ProductStockOverViewResponse findProductStockOverView(Long productId){
        ProductOrderSummaryQueryResult queryResult =
                backofficeProductQueryRepository.findProductOrdersSummary(productId)
                .orElseThrow(()->new BusinessException(ErrorCode.INVALID_PRODUCT_ID));
        return ProductStockOverViewResponse.from(queryResult);
    }


    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .initialStock(product.getInitialStock())
                .stock(product.getStock())
                .soldCount(product.getSoldCount())
                .maxPurchaseQuantity(product.getMaxPurchaseQuantity())
                .type(product.getType())
                .status(product.getStatus())
                .saleStartAt(product.getSaleStartAt())
                .saleEndAt(product.getSaleEndAt())
                .build();
    }

}
