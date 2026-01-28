package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.response.common.ApiResponse;
import com.wing.ecommercebackendwing.dto.response.payment.KHQRResponse;
import com.wing.ecommercebackendwing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payments", description = "KHQR Payment integration APIs")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/khqr/{orderId}")
    @Operation(summary = "Generate KHQR for an order")
    public ResponseEntity<KHQRResponse> generateKHQR(
            @Parameter(description = "Order ID", required = true, example = "a6c458c0-bfb9-48da-8d9e-fd6c6c8425ed")
            @PathVariable(name = "orderId") UUID orderId) {
        KHQRResponse response = paymentService.generateKHQR(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify/{md5}")
    @Operation(summary = "Verify payment by MD5 hash")
    public ResponseEntity<ApiResponse<String>> verifyPayment(
            @Parameter(description = "MD5 hash from payment callback", example = "a1b2c3d4e5f6", required = true)
            @PathVariable(name = "md5") @NotBlank String md5) {
        boolean isVerified = paymentService.verifyPaymentByMd5(md5);
        if (isVerified) {
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Payment verified successfully")
                    .build());
        } else {
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("Payment not yet complete")
                    .build());
        }
    }
}
