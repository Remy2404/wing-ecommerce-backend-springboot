package com.wing.ecommercebackendwing.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.wing.ecommercebackendwing.config.JwtProperties;

import java.time.Duration;

/**
 * In-memory cache for blacklisted JWT token IDs (JTIs).
 * 
 * DESIGN PRINCIPLES:
 * 1. BEST-EFFORT ONLY: This service provides a secondary safety layer for token revocation.
 *    It is NOT a hard guarantee of security in distributed or non-persistent environments.
 * 2. SCOPE: Intended ONLY for explicit security events:
 *    - User Logout
 *    - Password Change (current token)
 *    - Administrative Account Revocation (current token if possible)
 * 3. NON-PERSISTENT: Data is lost on backend restart. This is intentional to avoid 
 *    overhead and complexity. Tokens naturally expire via TTL.
 * 4. UX FOCUS: This service should NEVER block core application functionality. If the 
 *    cache is lost or the token is missing from the local instance, natural JWT expiry
 *    remains the fallback.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final Cache<String, Boolean> blacklist;

    public TokenBlacklistService(JwtProperties jwtProperties) {
        long jwtExpirationMs = jwtProperties.getAccessToken().getExpiration();
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
