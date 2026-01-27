package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.WingPointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wing-points")
@RequiredArgsConstructor
@Tag(name = "Wing Points", description = "Loyalty program and points management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class WingPointsController {

    private final WingPointsService wingPointsService;

    @GetMapping("/balance")
    @Operation(summary = "Get user's current points balance")
    public ResponseEntity<Map<String, Object>> getBalance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer balance = wingPointsService.getPointsBalance(userDetails.getUserId());
        Map<String, Object> response = new HashMap<>();
        response.put("balance", balance);
        response.put("userId", userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
