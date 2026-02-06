package com.wing.ecommercebackendwing.controller;

import com.wing.ecommercebackendwing.dto.request.user.UpdateProfileRequest;
import com.wing.ecommercebackendwing.dto.response.auth.UserResponse;
import com.wing.ecommercebackendwing.dto.response.user.UserStatsResponse;
import com.wing.ecommercebackendwing.security.CustomUserDetails;
import com.wing.ecommercebackendwing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = userService.getUserById(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @RequestBody UpdateProfileRequest updateRequest) {
        UserResponse response = userService.updateProfile(userDetails.getUserId(), updateRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and update user avatar")
    public ResponseEntity<UserResponse> uploadAvatar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                     @RequestParam("file") MultipartFile file) {
        UserResponse response = userService.updateAvatar(userDetails.getUserId(), file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user dashboard statistics")
    public ResponseEntity<UserStatsResponse> getStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getDashboardStats(userDetails.getUserId()));
    }
}
