package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.address.CreateAddressRequest;
import com.wing.ecommercebackendwing.dto.request.address.UpdateAddressRequest;
import com.wing.ecommercebackendwing.dto.response.order.AddressResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.AddressService;
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
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "User shipping address APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get current user's addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(addressService.getUserAddresses(userDetails.getUserId()));
    }

    @PostMapping
    @Operation(summary = "Create new address")
    public ResponseEntity<AddressResponse> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.ok(addressService.createAddress(userDetails.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(userDetails.getUserId(), id, request));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set default address")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id) {
        return ResponseEntity.ok(addressService.setDefaultAddress(userDetails.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "id") UUID id) {
        addressService.deleteAddress(userDetails.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
