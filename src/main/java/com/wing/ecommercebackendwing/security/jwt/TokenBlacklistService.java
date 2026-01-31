package com.wing.ecommercebackendwing.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * In-memory cache for blacklisted JWT token IDs (JTIs).
 * Tokens are automatically evicted after the configured expiration time.
 * 
 * For multi-instance deployments, replace with Redis-based implementation.
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
