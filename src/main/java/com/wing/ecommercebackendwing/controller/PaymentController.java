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
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/khqr/{orderId}")
    @Operation(summary = "Generate KHQR for an order")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<KHQRResponse> generateKHQR(
            @Parameter(description = "Order ID", required = true)
            @PathVariable(name = "orderId") UUID orderId) {
        KHQRResponse response = paymentService.generateKHQR(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/md5/{md5}")
    @Operation(summary = "Verify payment by MD5")
    public ResponseEntity<ApiResponse<String>> verifyPaymentByMd5(
            @Parameter(description = "32 chars MD5 hash", required = true)
            @PathVariable(name = "md5") @NotBlank String md5) {
        String verificationResult = paymentService.verifyPaymentByMd5(md5);
        
        boolean isSuccess = verificationResult.startsWith("SUCCESS");
        
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(isSuccess)
                .message(verificationResult)
                .build());
    }
}
