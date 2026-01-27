package com.wing.ecommercebackendwing.dto.request.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionApplyRequest {
    @NotBlank
    private String code;
    @NotNull
    private UUID orderId;
}
