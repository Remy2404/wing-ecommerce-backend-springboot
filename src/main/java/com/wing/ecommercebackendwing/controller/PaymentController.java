package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.response.common.ApiResponse;
import com.wing.ecommercebackendwing.dto.response.payment.KHQRResponse;
import com.wing.ecommercebackendwing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<KHQRResponse> generateKHQR(@PathVariable UUID orderId) {
        KHQRResponse response = paymentService.generateKHQR(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify/{md5}")
    @Operation(summary = "Verify payment by MD5 hash")
    public ResponseEntity<ApiResponse<String>> verifyPayment(@PathVariable @NotBlank String md5) {
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
