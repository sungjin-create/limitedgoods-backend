package com.limitedgoods.limitedgoods.product.application.history;

import com.limitedgoods.limitedgoods.product.entity.*;
import com.limitedgoods.limitedgoods.product.repository.ProductHistoryRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductHistoryService {

    private final ProductHistoryRepository productHistoryRepository;

    public void recordInitial(Product product, User changedByUser) {
        ProductSnapshot snapshot = ProductSnapshot.from(product);

        productHistoryRepository.save(
                ProductHistory.create(
                        product,
                        changedByUser,
                        ProductHistoryType.CREATED,
                        snapshot.initialChanges(),
                        null,
                        product.getStatus(),
                        null,
                        product.getInitialStock(),
                        "상품 최초 생성"
                )
        );
    }

    public void recordProductUpdate(
            Product product,
            User changedByUser,
            ProductSnapshot before,
            ProductSnapshot after,
            String reason
    ) {
        Map<String, ProductFieldChange> changes = before.diff(after);

        // 변경 사항이 없으면 이력을 남기지 않는다.
        if (changes.isEmpty()) {
            return;
        }

        productHistoryRepository.save(
                ProductHistory.create(
                        product,
                        changedByUser,
                        ProductHistoryType.PRODUCT_UPDATED,
                        changes,
                        before.status(),
                        after.status(),
                        null,
                        null,
                        reason
                )
        );
    }

    public void recordStock(
            Product product,
            User changedByUser,
            int fromStock,
            int toStock,
            String reason
    ) {
        if (fromStock == toStock) {
            return;
        }

        Map<String, ProductFieldChange> changes = Map.of(
                "stock",
                new ProductFieldChange(fromStock, toStock)
        );

        productHistoryRepository.save(
                ProductHistory.create(
                        product,
                        changedByUser,
                        ProductHistoryType.STOCK_CHANGED,
                        changes,
                        null,
                        null,
                        fromStock,
                        toStock,
                        reason
                )
        );
    }

}
