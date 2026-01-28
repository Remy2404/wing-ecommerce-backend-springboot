package com.wing.ecommercebackendwing.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    @Schema(description = "Product name", example = "Wireless Headphones")
    @NotBlank(message = "Product name is required")
    private String name;

    @Schema(description = "URL-friendly product identifier", example = "wireless-headphones")
    @NotBlank(message = "Product slug is required")
    private String slug;

    @Schema(description = "Product description", example = "High-quality wireless headphones with noise cancellation")
    private String description;

    @Schema(description = "Product price in USD", example = "99.99", minimum = "0.01")
    @NotNull
    @Positive
    private BigDecimal price;

    @Schema(description = "Category ID")
    @NotNull
    private UUID categoryId;

    @Schema(description = "Available stock quantity", example = "100", minimum = "0")
    @Min(0)
    private Integer stock;

    @Schema(description = "Product image URLs", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private List<String> images;
}
