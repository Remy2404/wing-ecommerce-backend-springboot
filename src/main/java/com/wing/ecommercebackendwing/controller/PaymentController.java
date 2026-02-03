package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.response.common.ApiResponse;
import com.wing.ecommercebackendwing.dto.response.payment.KHQRResultDto;
import com.wing.ecommercebackendwing.dto.response.payment.PaymentVerificationResponse;
import com.wing.ecommercebackendwing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payments", description = "KHQR Payment integration APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/khqr/{orderId}")
    @Operation(summary = "Generate KHQR for an order")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<KHQRResultDto>> generateKHQR(
            @AuthenticationPrincipal com.wing.ecommercebackendwing.security.CustomUserDetails userDetails,
            @Parameter(description = "Order ID", required = true)
            @PathVariable(name = "orderId") UUID orderId) {
        
        KHQRResultDto result = paymentService.generateKHQR(orderId, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.<KHQRResultDto>builder()
                .success(true)
                .message("KHQR generated successfully")
                .data(result)
                .build());
    }

    @PostMapping("/verify/md5/{md5}")
    @Operation(summary = "Check transaction status with Bakong")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPaymentByMd5(
            @AuthenticationPrincipal com.wing.ecommercebackendwing.security.CustomUserDetails userDetails,
            @Parameter(description = "MD5 hash from KHQR generation", required = true)
            @PathVariable(name = "md5") @NotBlank String md5) {

        PaymentVerificationResponse status = paymentService.verifyPaymentByMd5(md5, userDetails.getUserId());
        
        return ResponseEntity.ok(ApiResponse.<PaymentVerificationResponse>builder()
                .success(status.isPaid())
                .message(status.getMessage())
                .data(status)
                .build());
    }
}
