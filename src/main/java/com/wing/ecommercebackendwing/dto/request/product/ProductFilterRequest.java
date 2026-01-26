package com.wing.ecommercebackendwing.dto.request.product;


import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductFilterRequest {
    private Integer page = 0;
    private Integer size = 20;
    private UUID categoryId;
    private String searchQuery;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
