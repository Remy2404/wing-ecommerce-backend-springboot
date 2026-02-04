package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.promotion.PromotionCreateRequest;
import com.wing.ecommercebackendwing.dto.request.promotion.PromotionUpdateRequest;
import com.wing.ecommercebackendwing.dto.response.promotion.PromotionResponse;
import com.wing.ecommercebackendwing.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@Tag(name = "Admin Promotions", description = "Admin promotion management APIs")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Create a new promotion")
    public ResponseEntity<PromotionResponse> createPromotion(@Valid @RequestBody PromotionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(request));
    }

    @GetMapping
    @Operation(summary = "List all promotions")
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID")
    public ResponseEntity<PromotionResponse> getPromotionById(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a promotion")
    public ResponseEntity<PromotionResponse> updatePromotion(@PathVariable UUID id, @Valid @RequestBody PromotionUpdateRequest request) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a promotion")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
