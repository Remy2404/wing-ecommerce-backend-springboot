package com.wing.ecommercebackendwing.dto.mapper;

import com.wing.ecommercebackendwing.dto.response.common.DeliveryResponse;
import com.wing.ecommercebackendwing.model.entity.Delivery;

public class DeliveryMapper {
    public static DeliveryResponse toResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrder() != null ? delivery.getOrder().getId() : null)
                .driverId(delivery.getDriver() != null ? delivery.getDriver().getId() : null)
                .status(delivery.getStatus() != null ? delivery.getStatus().name() : null)
                .driverNotes(delivery.getDriverNotes())
                .pickupTime(delivery.getPickupTime())
                .deliveredTime(delivery.getDeliveredTime())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}

