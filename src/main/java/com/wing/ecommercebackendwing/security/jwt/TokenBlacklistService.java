package com.wing.ecommercebackendwing.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * In-memory cache for blacklisted JWT token IDs (JTIs).
 * 
 * SCOPE:
 * - This service provides a BEST-EFFORT revocation mechanism for specific tokens.
 * - It is intended ONLY for explicit logout, password changes, or manual revocation.
 * - It is NOT a substitute for session management or primary authentication logic.
 * 
 * PERSISTENCE:
 * - This is intentionally NON-PERSISTENT.
 * - Data loss upon backend restart is acceptable and expected by design.
 * - Do NOT introduce Redis or DB dependency without architecture-level approval.
 * 
 * For multi-instance deployments requiring shared state, a Redis implementation is required.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final Cache<String, Boolean> blacklist;

    public TokenBlacklistService(@Value("${jwt.expiration:3600000}") long jwtExpirationMs) {
        this.blacklist = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(jwtExpirationMs))
                .maximumSize(10_000)
                .build();
        log.info("Token blacklist initialized with TTL: {}ms", jwtExpirationMs);
    }

    /**
     * Add a token JTI to the blacklist.
     */
    public void blacklist(String jti) {
        if (jti != null && !jti.isBlank()) {
            blacklist.put(jti, true);
            log.debug("Token blacklisted: {}", jti);
        }
    }

    /**
     * Check if a token JTI is blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return blacklist.getIfPresent(jti) != null;
    }
}
