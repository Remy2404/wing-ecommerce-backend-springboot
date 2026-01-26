package com.wing.ecommercebackendwing.dto.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OrderItemRequest {
    @NotNull
    private UUID productId;

    private UUID variantId;

    @Min(1)
    private Integer quantity;
}
