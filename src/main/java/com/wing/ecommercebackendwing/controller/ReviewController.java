package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.review.CreateReviewRequest;
import com.wing.ecommercebackendwing.dto.response.review.ReviewResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.ReviewService;
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
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create product review")
    public ResponseEntity<ReviewResponse> createReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable UUID productId) {
        List<ReviewResponse> response = reviewService.getProductReviews(productId);
        return ResponseEntity.ok(response);
    }
}
