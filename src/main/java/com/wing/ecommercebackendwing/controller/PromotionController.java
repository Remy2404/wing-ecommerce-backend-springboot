package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.promotion.PromotionApplyRequest;
import com.wing.ecommercebackendwing.dto.response.promotion.PromotionResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Promotion and discount management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/validate")
    @Operation(summary = "Validate a promotion code")
    public ResponseEntity<PromotionResponse> validatePromotion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Promotion code to validate", example = "SAVE20", required = true)
            @RequestParam(name = "code") String code) {
        return ResponseEntity.ok(promotionService.validatePromotion(code, userDetails.getUserId()));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply discount to an order")
    public ResponseEntity<PromotionResponse> applyDiscount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PromotionApplyRequest request) {
        return ResponseEntity.ok(promotionService.applyDiscount(userDetails.getUserId(), request));
    }
}
