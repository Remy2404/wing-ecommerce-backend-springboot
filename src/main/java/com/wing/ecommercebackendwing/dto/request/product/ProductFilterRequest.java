package com.wing.ecommercebackendwing.dto.request.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductFilterRequest {
    private UUID category;

    @PositiveOrZero
    private BigDecimal minPrice;

    @Positive
    private BigDecimal maxPrice;

    private String search;

    private String sortBy;

    @Min(0)
    private Integer page;

    @Min(1)
    @Max(100)
    private Integer limit;
}
