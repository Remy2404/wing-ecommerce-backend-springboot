 package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.order.CreateOrderRequest;
import com.wing.ecommercebackendwing.dto.response.order.OrderResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create new order")
    public ResponseEntity<OrderResponse> createOrder(Authentication authentication,
                                                     @Valid @RequestBody CreateOrderRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get user's orders")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderResponse> orders = orderService.getUserOrders(userDetails.getUserId(), page, size);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order detail by number")
    public ResponseEntity<OrderResponse> getOrderDetail(Authentication authentication,
                                                        @PathVariable String orderNumber) {
        UUID userId = UUID.fromString(authentication.getName());
        OrderResponse response = orderService.getOrderByNumber(userId, orderNumber);
        return ResponseEntity.ok(response);
    }
}
