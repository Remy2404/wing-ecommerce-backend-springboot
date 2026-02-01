package com.wing.ecommercebackendwing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryFeeService {

    private static final BigDecimal BASE_FEE = new BigDecimal("1.50");
    private static final BigDecimal TIER2_RATE = new BigDecimal("0.10");
    private static final BigDecimal TIER3_BASE = new BigDecimal("3.00");
    private static final BigDecimal TIER3_RATE = new BigDecimal("0.15");

    public BigDecimal calculateFee(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee;

        if (distanceKm.compareTo(new BigDecimal("5")) <= 0) {
            // 0-5 km: $1.50
            fee = BASE_FEE;
        } else if (distanceKm.compareTo(new BigDecimal("20")) <= 0) {
            // 5-20 km: $1.50 + (distance - 5) × $0.10
            BigDecimal extraDistance = distanceKm.subtract(new BigDecimal("5"));
            fee = BASE_FEE.add(extraDistance.multiply(TIER2_RATE));
        } else {
            // >20 km: $3.00 + (distance - 20) × $0.15
            BigDecimal extraDistance = distanceKm.subtract(new BigDecimal("20"));
            fee = TIER3_BASE.add(extraDistance.multiply(TIER3_RATE));
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
