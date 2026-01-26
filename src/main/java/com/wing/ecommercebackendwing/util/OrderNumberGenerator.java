package com.wing.ecommercebackendwing.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNumberGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);

    public String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = counter.getAndIncrement();
        return String.format("ORD-%s-%03d", date, sequence);
    }
}
