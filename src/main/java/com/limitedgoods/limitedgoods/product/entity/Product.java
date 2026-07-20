package com.limitedgoods.limitedgoods.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private int price;
    private int initialStock;
    private int stock;
    private int soldCount;
    private Integer maxPurchaseQuantity;

    @Enumerated(EnumType.STRING)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

    private LocalDateTime updatedAt;

    // 기존 필드와 메서드
    public boolean isPurchasableAt(LocalDateTime now) {
        if (status != ProductStatus.ACTIVE
                && status != ProductStatus.SCHEDULED) {
            return false;
        }

        if (status == ProductStatus.SCHEDULED
                && saleStartAt == null) {
            return false;
        }

        if (saleStartAt != null
                && now.isBefore(saleStartAt)) {
            return false;
        }

        if (saleEndAt != null
                && !now.isBefore(saleEndAt)) {
            return false;
        }

        return true;
    }

}
