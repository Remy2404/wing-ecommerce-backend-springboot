package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.paymentmethod.CreateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.request.paymentmethod.UpdateSavedPaymentMethodRequest;
import com.wing.ecommercebackendwing.dto.response.user.SavedPaymentMethodResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.SavedPaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Methods", description = "Saved payment method APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class SavedPaymentMethodController {

    private final SavedPaymentMethodService savedPaymentMethodService;

    @GetMapping
    @Operation(summary = "Get current user's saved payment methods")
    public ResponseEntity<List<SavedPaymentMethodResponse>> getMethods(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(savedPaymentMethodService.getUserPaymentMethods(userDetails.getUserId()));
    }

    @PostMapping
    @Operation(summary = "Save a new payment method")
    public ResponseEntity<SavedPaymentMethodResponse> createMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateSavedPaymentMethodRequest request) {
        return ResponseEntity.ok(savedPaymentMethodService.createPaymentMethod(userDetails.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a saved payment method")
    public ResponseEntity<SavedPaymentMethodResponse> updateMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateSavedPaymentMethodRequest request) {
        return ResponseEntity.ok(savedPaymentMethodService.updatePaymentMethod(userDetails.getUserId(), id, request));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set default payment method")
    public ResponseEntity<SavedPaymentMethodResponse> setDefaultMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id) {
        return ResponseEntity.ok(savedPaymentMethodService.setDefaultPaymentMethod(userDetails.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved payment method")
    public ResponseEntity<Void> deleteMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id) {
        savedPaymentMethodService.deletePaymentMethod(userDetails.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
