package com.wing.ecommercebackendwing.dto.request.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UpdateCartItemRequest {
    @NotNull
    private UUID cartItemId;

    @Min(1)
    private Integer quantity;
}
