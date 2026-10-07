package com.interviewprep.ratelimit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * Bound from app.rate-limit.* (env RATE_LIMIT_REQUESTS / RATE_LIMIT_WINDOW), so the limit
 * and window change without a code change. Invalid values fail at startup.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int requests,
        // A bare number means seconds ("60" = 60s, not 60ms); "1m" or "10s" also work.
        @DurationUnit(ChronoUnit.SECONDS) Duration window) {

    public RateLimitProperties {
        if (requests < 1) {
            throw new IllegalArgumentException("app.rate-limit.requests must be at least 1");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("app.rate-limit.window must be a positive duration");
        }
    }
}
