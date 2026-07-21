package com.limitedgoods.limitedgoods.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Builder
public record OrderRequestDto (

    @NotBlank
    @Size(max = 255)
    String checkoutToken,

    @NotEmpty
    @Valid
    @Size(max = 50)
    List<OrderItemRequestDto> items,

    @Size(max = 100)
    String admissionToken
) {

}
