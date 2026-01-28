package com.wing.ecommercebackendwing.dto.request.order;

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
public class OrderItemRequest {
    @Schema(description = "Product ID")
    @NotNull
    private UUID productId;

    @Schema(description = "Product variant ID (optional)")
    private UUID variantId;

    @Schema(description = "Quantity to order", example = "2", minimum = "1")
    @Min(1)
    private Integer quantity;
}
