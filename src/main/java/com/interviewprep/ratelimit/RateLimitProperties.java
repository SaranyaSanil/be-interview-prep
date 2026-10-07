package com.interviewprep.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.rate-limit.* (env RATE_LIMIT_REQUESTS / RATE_LIMIT_WINDOW), so the limit
 * and window change without a code change. Invalid values fail at startup.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int requests, Duration window) {

    public RateLimitProperties {
        if (requests < 1) {
            throw new IllegalArgumentException("app.rate-limit.requests must be at least 1");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("app.rate-limit.window must be a positive duration");
        }
    }
}
