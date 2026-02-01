package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.common.Pagination;
import com.wing.ecommercebackendwing.dto.response.order.OrderItemResponse;
import com.wing.ecommercebackendwing.dto.response.order.OrderListResponse;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.model.entity.Order;
import com.wing.ecommercebackendwing.model.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderMapper::toOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .paymentStatus(order.getPayment() != null ? order.getPayment().getStatus().name() : "PENDING")
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    public static OrderListResponse toListResponse(List<Order> orders, Pagination pagination) {
        List<OrderResponse> orderResponses = orders.stream()
                .map(OrderMapper::toResponse)
                .collect(Collectors.toList());
        return OrderListResponse.builder()
                .orders(orderResponses)
                .pagination(pagination)
                .build();
    }

    private static OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        BigDecimal subtotal = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProductName())
                .variantId(orderItem.getVariant() != null ? orderItem.getVariant().getId() : null)
                .variantName(orderItem.getVariantName())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getUnitPrice())
                .subtotal(subtotal)
                .productSlug(orderItem.getProduct().getSlug())
                .productImage(extractFirstImage(orderItem.getProduct().getImages()))
                .build();
    }

    private static String extractFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return null;
        }
        try {
            String clean = imagesJson.trim();
            if (clean.startsWith("[\"") && clean.endsWith("\"]")) {
                int endIndex = clean.indexOf("\",");
                if (endIndex == -1) {
                    endIndex = clean.lastIndexOf("\"]");
                }
                return clean.substring(2, endIndex);
            } else if (clean.startsWith("[") && clean.endsWith("]")) {
                 return null;
            }
            return clean;
        } catch (Exception e) {
            return null;
        }
    }
}
