package com.wing.ecommercebackendwing.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.wing.ecommercebackendwing.exception.custom.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PhoneNumberService {
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private final String defaultRegion;
    private final Set<String> isoCountries = Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());

    public PhoneNumberService(@Value("${app.phone.default-region:KH}") String defaultRegion) {
        this.defaultRegion = normalizeIsoRegion(defaultRegion);
    }

    public String normalizeToE164(String rawPhone, String countryOrIso) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            throw new BadRequestException("Invalid phone number");
        }

        String value = rawPhone.trim();
        String region = resolveRegion(countryOrIso);

        try {
            Phonenumber.PhoneNumber parsed = value.startsWith("+")
                    ? phoneNumberUtil.parse(value, null)
                    : phoneNumberUtil.parse(value, region);

            if (!phoneNumberUtil.isPossibleNumber(parsed)) {
                throw new BadRequestException("Invalid phone number");
            }

            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException exception) {
            throw new BadRequestException("Invalid phone number");
        }
    }

    private String resolveRegion(String countryOrIso) {
        if (countryOrIso == null || countryOrIso.trim().isEmpty()) {
            return defaultRegion;
        }

        String normalized = countryOrIso.trim();
        if (normalized.length() == 2) {
            String iso = normalizeIsoRegion(normalized);
            if (isoCountries.contains(iso)) {
                return iso;
            }
        }

        String lookup = normalized.toLowerCase(Locale.ENGLISH);
        for (String iso : isoCountries) {
            Locale locale = new Locale("", iso);
            String displayCountry = locale.getDisplayCountry(Locale.ENGLISH);
            if (displayCountry != null && displayCountry.toLowerCase(Locale.ENGLISH).equals(lookup)) {
                return iso;
            }
        }

        return defaultRegion;
    }

    private String normalizeIsoRegion(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "KH";
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }
}
