package com.wing.ecommercebackendwing.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    @Schema(description = "Product ID to add to cart")
    @NotNull
    private UUID productId;

    @Schema(description = "Product variant ID (optional)", example = "null")
    private UUID variantId;

    @Schema(description = "Quantity to add", example = "1", minimum = "1")
    @Min(1)
    private Integer quantity;
}
