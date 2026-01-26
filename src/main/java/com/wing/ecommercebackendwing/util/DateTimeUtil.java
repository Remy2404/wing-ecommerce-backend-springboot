package com.wing.ecommercebackendwing.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String formatDate(Instant instant) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DATE_FORMATTER);
    }

    public String formatDateTime(Instant instant) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public Instant parseDate(String dateString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateString + " 00:00:00", DATE_TIME_FORMATTER);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public Instant parseDateTime(String dateTimeString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
