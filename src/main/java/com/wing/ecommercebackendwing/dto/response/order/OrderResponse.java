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
    private List<OrderItemResponse> items;
    private Instant createdAt;
}
