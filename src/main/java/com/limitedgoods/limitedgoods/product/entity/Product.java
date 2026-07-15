package com.limitedgoods.limitedgoods.product.entity;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
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
    private int stock;

    @Enumerated(EnumType.STRING)
    private ProductType type;

    @Column(nullable = false)
    private boolean visible = true;

    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        this.stock -= quantity;
    }
}
