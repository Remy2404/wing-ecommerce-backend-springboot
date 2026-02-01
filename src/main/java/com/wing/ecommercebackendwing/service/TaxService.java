package com.wing.ecommercebackendwing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

    public BigDecimal calculateTax(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
