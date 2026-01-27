package com.wing.ecommercebackendwing.dto.request.common;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    @NotBlank
    private String type;
    private UUID relatedId;
}
