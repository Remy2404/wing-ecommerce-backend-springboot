package com.wing.ecommercebackendwing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateWindow> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${security.rate-limit.auth.max-requests:5}")
    private int authMaxRequests;

    @Value("${security.rate-limit.auth.window-seconds:60}")
    private long authWindowSeconds;

    @Value("${security.rate-limit.payment-verify.max-requests:15}")
    private int paymentVerifyMaxRequests;

    @Value("${security.rate-limit.payment-verify.window-seconds:60}")
    private long paymentVerifyWindowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        RatePolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = policy.name + ":" + clientIp;

        if (!allowRequest(key, policy)) {
            long retryAfterSeconds = Math.max(policy.windowSeconds, 1);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", "Too many requests. Please retry later.",
                    "code", "RATE_LIMIT_EXCEEDED"
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RatePolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && (
                "/api/auth/login".equals(path)
                        || "/api/auth/register".equals(path)
                        || "/api/auth/forgot-password".equals(path)
        )) {
            return new RatePolicy("AUTH", authMaxRequests, authWindowSeconds);
        }

        if (HttpMethod.POST.matches(method) && path.startsWith("/api/payments/verify/")) {
            return new RatePolicy("PAYMENT_VERIFY", paymentVerifyMaxRequests, paymentVerifyWindowSeconds);
        }

        return null;
    }

    private boolean allowRequest(String key, RatePolicy policy) {
        long now = System.currentTimeMillis();
        long windowMillis = policy.windowSeconds * 1000L;

        RateWindow window = counters.computeIfAbsent(key, ignored -> new RateWindow(now));
        synchronized (window) {
            if (now - window.windowStartMillis >= windowMillis) {
                window.windowStartMillis = now;
                window.requestCount = 0;
            }
            if (window.requestCount >= policy.maxRequests) {
                return false;
            }
            window.requestCount++;
            return true;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return (commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class RatePolicy {
        private final String name;
        private final int maxRequests;
        private final long windowSeconds;

        private RatePolicy(String name, int maxRequests, long windowSeconds) {
            this.name = name;
            this.maxRequests = Math.max(1, maxRequests);
            this.windowSeconds = Math.max(1, windowSeconds);
        }
    }

    private static final class RateWindow {
        private long windowStartMillis;
        private int requestCount;

        private RateWindow(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
            this.requestCount = 0;
        }
    }
}
