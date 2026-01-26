package com.wing.ecommercebackendwing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard stats")
    public ResponseEntity<?> getDashboardStats() {
        // Return basic dashboard statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 0);
        stats.put("totalOrders", 0);
        stats.put("totalRevenue", 0.0);
        stats.put("timestamp", Instant.now());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/orders")
    @Operation(summary = "Get all orders for admin")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Return paginated orders list
        Map<String, Object> response = new HashMap<>();
        response.put("orders", new ArrayList<>());
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", 0);
        return ResponseEntity.ok(response);
    }
}
