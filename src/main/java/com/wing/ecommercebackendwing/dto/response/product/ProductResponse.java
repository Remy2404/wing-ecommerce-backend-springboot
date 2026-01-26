package com.wing.ecommercebackendwing.dto.response.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private Integer stock;
    private String images;
    private Double rating;
}
