package com.interviewprep.ratelimit;

import java.time.Duration;

/** Outcome of one request: either allowed (with remaining quota) or rejected (with wait time). */
public record RateLimitDecision(boolean allowed, int remaining, Duration retryAfter) {

    static RateLimitDecision allow(int remaining) {
        return new RateLimitDecision(true, remaining, Duration.ZERO);
    }

    static RateLimitDecision reject(Duration retryAfter) {
        return new RateLimitDecision(false, 0, retryAfter);
    }

    /** Whole seconds to wait, rounded up so the client never retries too early. */
    public long retryAfterSeconds() {
        long millis = retryAfter.toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}
