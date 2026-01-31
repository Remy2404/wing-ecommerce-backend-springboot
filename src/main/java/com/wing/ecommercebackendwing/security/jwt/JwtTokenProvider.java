package com.wing.ecommercebackendwing.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .setIssuer("wing-api")
                .setAudience("wing-client")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer("wing-api")
                .requireAudience("wing-client")
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer("wing-api")
                    .requireAudience("wing-client")
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }
}

