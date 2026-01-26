package com.wing.ecommercebackendwing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard stats")
    public ResponseEntity<Object> getDashboard() {
        // TODO: Return dashboard statistics
        return ResponseEntity.ok("Admin dashboard");
    }

    @GetMapping("/orders")
    @Operation(summary = "Get all orders for admin")
    public ResponseEntity<Object> getAllOrders() {
        // TODO: Return all orders with pagination
        return ResponseEntity.ok("All orders");
    }
}
