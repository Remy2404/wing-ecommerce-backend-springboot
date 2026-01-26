package com.wing.ecommercebackendwing.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateOrderRequest {
    private List<OrderItemRequest> items;

    private ShippingAddressRequest shippingAddress;

    @NotBlank
    private String paymentMethod;
}
