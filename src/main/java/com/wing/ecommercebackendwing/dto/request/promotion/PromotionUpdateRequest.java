package com.wing.ecommercebackendwing.dto.request.promotion;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PromotionUpdateRequest {

    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    private String description;

    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Type must be PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Value must be greater than 0")
    private BigDecimal value;

    @DecimalMin(value = "0.0", message = "Minimum order amount must be non-negative")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "Max discount must be non-negative")
    private BigDecimal maxDiscount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per user limit must be at least 1")
    private Integer perUserLimit;

    private Instant startDate;

    private Instant endDate;

    private Boolean isActive;

    private String applicableCategories;

    private String applicableMerchants;
}
