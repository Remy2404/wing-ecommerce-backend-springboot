package com.wing.ecommercebackendwing.dto.response.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private String status;
    private BigDecimal total;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private BigDecimal tax;
    private String paymentStatus;
    private List<OrderItemResponse> items;
    private Instant createdAt;
}
