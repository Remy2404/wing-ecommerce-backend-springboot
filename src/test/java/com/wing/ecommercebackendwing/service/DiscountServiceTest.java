package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.model.entity.Promotion;
import com.wing.ecommercebackendwing.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private DiscountService discountService;

    @Test
    void calculateDiscount_shouldUseDatabasePromotionPercentage() {
        Promotion promotion = new Promotion();
        promotion.setCode("KONKHER");
        promotion.setType("PERCENTAGE");
        promotion.setValue(new BigDecimal("15"));
        promotion.setMinOrderAmount(new BigDecimal("0"));
        promotion.setMaxDiscount(new BigDecimal("100"));
        promotion.setIsActive(true);
        promotion.setStartDate(Instant.now().minusSeconds(3600));
        promotion.setEndDate(Instant.now().plusSeconds(3600));

        when(promotionRepository.findByCodeIgnoreCase("KONKHER")).thenReturn(Optional.of(promotion));

        BigDecimal discount = discountService.calculateDiscount(new BigDecimal("200.00"), "KONKHER");
        assertEquals(new BigDecimal("30.00"), discount);
    }

    @Test
    void calculateDiscount_shouldReturnZeroWhenPromotionCodeNotFound() {
        when(promotionRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.empty());

        BigDecimal discount = discountService.calculateDiscount(new BigDecimal("200.00"), "SAVE10");
        assertEquals(BigDecimal.ZERO, discount);
    }
}
