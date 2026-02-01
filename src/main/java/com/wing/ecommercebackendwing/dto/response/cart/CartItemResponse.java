package com.wing.ecommercebackendwing.dto.response.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CartItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID variantId;
    private String variantName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String productImage;
    private String productSlug;
}
