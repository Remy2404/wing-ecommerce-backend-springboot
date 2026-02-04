package com.wing.ecommercebackendwing.dto.request.promotion;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PromotionCreateRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    private String description;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Type must be PERCENTAGE or FIXED_AMOUNT")
    private String type;

    @NotNull(message = "Value is required")
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

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotNull(message = "End date is required")
    private Instant endDate;

    private Boolean isActive = true; // Default to active

    private String applicableCategories;

    private String applicableMerchants;
}
