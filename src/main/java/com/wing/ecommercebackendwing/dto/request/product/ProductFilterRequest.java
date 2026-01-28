package com.wing.ecommercebackendwing.dto.request.product;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductFilterRequest {
    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer size = 20;
    
    private UUID categoryId;
    private String searchQuery;
    
    @Min(value = 0, message = "Minimum price must be 0 or greater")
    private BigDecimal minPrice;
    
    @Min(value = 0, message = "Maximum price must be 0 or greater")
    private BigDecimal maxPrice;
}
