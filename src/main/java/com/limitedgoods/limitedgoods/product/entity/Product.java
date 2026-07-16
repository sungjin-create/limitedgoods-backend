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

    @Column(nullable = false)
    private boolean visible = true;

    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

}
