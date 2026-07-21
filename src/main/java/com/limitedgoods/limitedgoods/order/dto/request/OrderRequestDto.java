package com.limitedgoods.limitedgoods.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {

    @NotBlank
    @Size(max = 255)
    private String checkoutToken;

    @NotEmpty
    @Valid
    @Size(max = 50)
    private List<OrderItemsListDto> items;

    @Size(max = 100)
    private String admissionToken;
}
