package com.limitedgoods.limitedgoods.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
}
