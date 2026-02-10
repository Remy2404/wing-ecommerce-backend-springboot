package com.wing.ecommercebackendwing.service;

import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberServiceTest {
    private PhoneNumberService phoneNumberService;

    @BeforeEach
    void setUp() {
        phoneNumberService = new PhoneNumberService("KH");
    }

    @Test
    void normalizeToE164_shouldAcceptLocalNumberWithRegion() {
        String normalized = phoneNumberService.normalizeToE164("09620264091", "KH");
        assertEquals("+8559620264091", normalized);
    }

    @Test
    void normalizeToE164_shouldAcceptInternationalNumber() {
        String normalized = phoneNumberService.normalizeToE164("+447911123456", "GB");
        assertEquals("+447911123456", normalized);
    }

    @Test
    void normalizeToE164_shouldRejectTooShortNumber() {
        assertThrows(BadRequestException.class,
                () -> phoneNumberService.normalizeToE164("1234", "US"));
    }

    @Test
    void normalizeToE164_shouldRejectNumberWithLetters() {
        assertThrows(BadRequestException.class,
                () -> phoneNumberService.normalizeToE164("09abc264091", "KH"));
    }

    @Test
    void normalizeToE164_shouldAcceptNumberWithCommonSeparators() {
        String normalized = phoneNumberService.normalizeToE164("096-202-64091", "KH");
        assertEquals("+8559620264091", normalized);
    }
}
