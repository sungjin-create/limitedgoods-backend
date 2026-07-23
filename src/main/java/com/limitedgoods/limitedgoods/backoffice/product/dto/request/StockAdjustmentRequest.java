package com.limitedgoods.limitedgoods.backoffice.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentRequest {

    @NotNull(message = "상품ID는 필수입니다.")
    @Positive(message = "상품ID는 1 이상이어야 합니다.")
    private Long id;

    @NotNull(message = "재고 조정 방식은 필수입니다.")
    private StockAdjustmentType adjustmentType;

    @NotNull(message = "재고 수량은 필수입니다.")
    @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "재고 조정 사유는 필수입니다.")
    @Size(max = 1000, message = "재고 조정 사유는 1,000자 이하여야 합니다.")
    private String reason;

}
