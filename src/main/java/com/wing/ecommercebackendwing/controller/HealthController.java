package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.response.common.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check application health (GET)")
    public ResponseEntity<MessageResponse> getHealth() {
        return ResponseEntity.ok(MessageResponse.builder()
                .success(true)
                .message("Service is healthy")
                .build());
    }

    @RequestMapping(method = RequestMethod.HEAD)
    @Operation(summary = "Check application health (HEAD)")
    public ResponseEntity<Void> headHealth() {
        return ResponseEntity.ok().build();
    }
}
