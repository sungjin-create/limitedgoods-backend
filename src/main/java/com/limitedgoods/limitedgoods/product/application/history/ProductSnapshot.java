package com.limitedgoods.limitedgoods.product.application.history;

import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductFieldChange;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.entity.ProductType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ProductSnapshot(
        String name,
        String description,
        int price,
        Integer maxPurchaseQuantity,
        ProductType type,
        ProductStatus status,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt
) {
    public static ProductSnapshot from(Product product) {
        return new ProductSnapshot(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getMaxPurchaseQuantity(),
                product.getType(),
                product.getStatus(),
                product.getSaleStartAt(),
                product.getSaleEndAt()
        );
    }

    public Map<String, ProductFieldChange> diff(ProductSnapshot after) {
        Map<String, ProductFieldChange> changes = new LinkedHashMap<>();

        addIfChanged(changes, "name", name, after.name);
        addIfChanged(changes, "description", description, after.description);
        addIfChanged(changes, "price", price, after.price);
        addIfChanged(changes, "maxPurchaseQuantity", maxPurchaseQuantity, after.maxPurchaseQuantity);
        addIfChanged(changes, "type", type, after.type);
        addIfChanged(changes, "status", status, after.status);
        addIfChanged(changes, "saleStartAt", saleStartAt, after.saleStartAt);
        addIfChanged(changes, "saleEndAt", saleEndAt, after.saleEndAt);

        return changes;
    }

    public Map<String, ProductFieldChange> initialChanges() {
        Map<String, ProductFieldChange> changes = new LinkedHashMap<>();

        addIfChanged(changes, "name", null, name);
        addIfChanged(changes, "description", null, description);
        addIfChanged(changes, "price", null, price);
        addIfChanged(changes, "maxPurchaseQuantity", null, maxPurchaseQuantity);
        addIfChanged(changes, "type", null, type);
        addIfChanged(changes, "status", null, status);
        addIfChanged(changes, "saleStartAt", null, saleStartAt);
        addIfChanged(changes, "saleEndAt", null, saleEndAt);

        return changes;
    }

    private static void addIfChanged(
            Map<String, ProductFieldChange> changes,
            String fieldName,
            Object from,
            Object to
    ) {
        if (!Objects.equals(from, to)) {
            changes.put(fieldName, new ProductFieldChange(from, to));
        }
    }
}
