package com.wing.ecommercebackendwing.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.UUID;

/**
 * Custom UUID deserializer that can handle both standard UUID format (with hyphens)
 * and compact UUID format (32 characters without hyphens).
 *
 * Examples:
 * - Standard: "aaad6ba8-9e10-45b3-ba46-fa8542555882"
 * - Compact: "aaad6ba89e1045b3ba46fa8542555882"
 */
public class UuidDeserializer extends StdDeserializer<UUID> {

    public UuidDeserializer() {
        super(UUID.class);
    }

    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String uuidString = p.getText().trim();

        if (uuidString.length() == 32) {
            // Convert compact format to standard format by inserting hyphens
            uuidString = uuidString.substring(0, 8) + "-" +
                        uuidString.substring(8, 12) + "-" +
                        uuidString.substring(12, 16) + "-" +
                        uuidString.substring(16, 20) + "-" +
                        uuidString.substring(20, 32);
        }

        return UUID.fromString(uuidString);
    }
}
