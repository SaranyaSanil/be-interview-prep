package com.interviewprep.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sliding-window log: for each key, the times of the requests allowed during the last window.
 * A request is allowed only while fewer than {@code limit} of them are inside the window, so
 * no window of that length ever contains more than {@code limit} requests (a fixed per-minute
 * counter would allow 2x the limit across a minute boundary).
 *
 * <p>The check-and-record step runs inside {@link ConcurrentMap#compute}, which is atomic per
 * key: simultaneous requests for one key are serialised, while different keys don't block
 * each other. State is in memory, so the limit is per application instance.
 */
@Component
public class SlidingWindowRateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration window;
    private final Clock clock;

    public SlidingWindowRateLimiter(RateLimitProperties properties, Clock clock) {
        this.limit = properties.requests();
        this.window = properties.window();
        this.clock = clock;
    }

    public RateLimitDecision tryAcquire(String key) {
        RateLimitDecision[] decision = new RateLimitDecision[1];
        requestLog.compute(key, (k, existing) -> {
            Instant now = clock.instant();
            Instant windowStart = now.minus(window);
            Deque<Instant> log = existing != null ? existing : new ArrayDeque<>();
            pruneBefore(log, windowStart);
            if (log.size() < limit) {
                log.addLast(now);
                decision[0] = RateLimitDecision.allow(limit - log.size());
            } else {
                // The oldest request leaves the window at oldest + window, i.e. after (oldest - windowStart).
                decision[0] = RateLimitDecision.reject(Duration.between(windowStart, log.peekFirst()));
            }
            return log;
        });
        return decision[0];
    }

    /**
     * Removes keys with no requests left in the window, so memory doesn't grow with every key
     * ever seen (e.g. a client sending a new random key per request). computeIfPresent keeps
     * this atomic with tryAcquire; returning null removes the entry.
     */
    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-interval:1m}")
    public void evictIdleKeys() {
        Instant windowStart = clock.instant().minus(window);
        for (String key : requestLog.keySet()) {
            requestLog.computeIfPresent(key, (k, log) -> {
                pruneBefore(log, windowStart);
                return log.isEmpty() ? null : log;
            });
        }
    }

    int trackedKeys() {
        return requestLog.size();
    }

    // Drops requests that are no longer inside the window (oldest first).
    private static void pruneBefore(Deque<Instant> log, Instant windowStart) {
        while (!log.isEmpty() && !log.peekFirst().isAfter(windowStart)) {
            log.pollFirst();
        }
    }
}
