package com.limitedgoods.limitedgoods.product.dto;

import com.limitedgoods.limitedgoods.product.entity.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductRegisterRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "양수형태만 가능합니다.")
    private int price;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "하나이상의 숫자만 가능합니다.")
    private int stock;

    private int initialStock;

    @NotNull(message = "type은 필수입니다.")
    private ProductType type;

    @NotNull(message = "상품의 보여짐 여부는 필수입니다.")
    private boolean visible;

    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

}
