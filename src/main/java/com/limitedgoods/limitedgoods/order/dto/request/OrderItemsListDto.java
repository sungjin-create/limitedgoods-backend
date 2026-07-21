package com.limitedgoods.limitedgoods.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemsListDto {

    @NotNull(message = "상품ID는 필수입니다.")
    @Positive(message = "ID는 양수만 가능합니다.")
    private Long productId;

    @Positive(message = "수량은 양수만 가능합니다.")
    private int quantity;
}
