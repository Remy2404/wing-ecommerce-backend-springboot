package com.wing.ecommercebackendwing.dto.response.common;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DeliveryResponse {
    private UUID id;
    private UUID orderId;
    private UUID driverId;
    private String status;
    private String driverNotes;
    private Instant pickupTime;
    private Instant deliveredTime;
    private Instant createdAt;
    private Instant updatedAt;
}

