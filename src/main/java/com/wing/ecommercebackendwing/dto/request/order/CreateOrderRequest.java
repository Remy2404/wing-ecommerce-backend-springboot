package com.wing.ecommercebackendwing.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @Schema(description = "List of items to order")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Schema(description = "Shipping address for delivery")
    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressRequest shippingAddress;

    @Schema(description = "Payment method", example = "KHQR", allowableValues = {"KHQR", "CASH_ON_DELIVERY"})
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Schema(description = "Coupon code for discount", example = "SAVE10")
    private String couponCode;

    @Schema(description = "Delivery distance in kilometers", example = "10.5")
    private java.math.BigDecimal distanceKm;
}
