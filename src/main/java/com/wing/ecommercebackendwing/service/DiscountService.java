package com.wing.ecommercebackendwing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DiscountService {

    public static final String COUPON_SAVE10 = "SAVE10";
    public static final String COUPON_FLASH50 = "FLASH50";
    public static final String COUPON_FREEDEL = "FREEDEL";

    public BigDecimal calculateDiscount(BigDecimal subtotal, String couponCode) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || couponCode == null) {
            return BigDecimal.ZERO;
        }

        String code = couponCode.toUpperCase().trim();
        
        switch (code) {
            case COUPON_SAVE10:
                return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            case COUPON_FLASH50:
                return new BigDecimal("50.00");
            case COUPON_FREEDEL:
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    public boolean isFreeDeliveryCoupon(String couponCode) {
        return COUPON_FREEDEL.equalsIgnoreCase(couponCode);
    }
}
