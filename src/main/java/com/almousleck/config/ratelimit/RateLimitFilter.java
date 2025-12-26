package com.almousleck.config.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private final RateLimitConfig rateLimitConfig;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        
        // Skip rate limiting for auth endpoints
        if (path.startsWith("/api/v1/auth/")) {
            log.debug("Skipping rate limit for auth endpoint: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // Apply rate limiting to /api/** and /actuator/**
        if (path.startsWith("/api/") || path.startsWith("/actuator/")) {
            String clientIp = getClientIp(request);
            log.info("🔥 RateLimitFilter invoked for IP: {} on path: {}", clientIp, path);
            
            byte[] bucketKey = ("rate_limit:" + clientIp).getBytes();

            Bucket bucket = proxyManager.builder().build(bucketKey, bucketConfiguration);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getRequestsPerMinute()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
                chain.doFilter(request, response);
            } else {
                long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(waitForRefill));
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getRequestsPerMinute()));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write(
                        String.format("{\"error\":\"Too many requests\",\"retryAfter\":%d}", waitForRefill)
                );

                log.warn("🚫 Rate limit exceeded for IP: {}", clientIp);
            }
        } else {
            // Not an API or actuator path, skip rate limiting
            chain.doFilter(request, response);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

