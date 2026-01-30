package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.model.enums.DeliveryStatus;
import com.wing.ecommercebackendwing.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Delivery tracking and management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/{deliveryId}/assign/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a driver to a delivery (Admin only)")
    public ResponseEntity<?> assignDriver(
            @PathVariable(name = "deliveryId") UUID deliveryId,
            @PathVariable(name = "driverId") UUID driverId) {
        return ResponseEntity.ok(deliveryService.assignDriver(deliveryId, driverId));
    }

    @PutMapping("/{deliveryId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @Operation(summary = "Update delivery status (Admin/Driver only)")
    public ResponseEntity<Void> updateStatus(
            @PathVariable(name = "deliveryId") UUID deliveryId,
            @RequestParam(name = "status") DeliveryStatus status,
            @RequestParam(name = "notes", required = false) String notes) {
        deliveryService.updateDeliveryStatus(deliveryId, status, notes);
        return ResponseEntity.ok().build();
    }
}
