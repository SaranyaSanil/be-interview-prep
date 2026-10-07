package com.interviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final SlidingWindowRateLimiter limiter =
            new SlidingWindowRateLimiter(new RateLimitProperties(10, Duration.ofMinutes(1)), clock);

    @Test
    void allowsTenRequestsThenRejectsTheEleventh() {
        for (int i = 0; i < 10; i++) {
            RateLimitDecision decision = limiter.tryAcquire("key");
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.remaining()).isEqualTo(9 - i);
        }

        RateLimitDecision eleventh = limiter.tryAcquire("key");

        assertThat(eleventh.allowed()).isFalse();
        assertThat(eleventh.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void retryAfterIsTimeUntilTheOldestRequestLeavesTheWindow() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("key");
            clock.advance(Duration.ofSeconds(5)); // requests at 0s, 5s, ..., 45s
        }
        // now = 50s: the request at 0s leaves the window at 60s
        RateLimitDecision rejected = limiter.tryAcquire("key");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(10);

        clock.advance(Duration.ofSeconds(9).plusMillis(999));
        assertThat(limiter.tryAcquire("key").allowed()).isFalse();

        clock.advance(Duration.ofMillis(1)); // exactly 60s after the first request
        assertThat(limiter.tryAcquire("key").allowed()).isTrue();
        assertThat(limiter.tryAcquire("key").allowed()).isFalse(); // the 5s request is still inside
    }

    @Test
    void noBurstAcrossAMinuteBoundary() {
        clock.advance(Duration.ofSeconds(59));
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("key").allowed()).isTrue();
        }
        clock.advance(Duration.ofSeconds(2)); // a fixed per-minute counter would reset here

        assertThat(limiter.tryAcquire("key").allowed()).isFalse();
    }

    @Test
    void keysAreLimitedIndependently() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("client-a");
        }

        assertThat(limiter.tryAcquire("client-a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("client-b").allowed()).isTrue();
    }

    @Test
    void simultaneousRequestsNeverExceedTheLimit() throws Exception {
        SlidingWindowRateLimiter realTimeLimiter =
                new SlidingWindowRateLimiter(new RateLimitProperties(10, Duration.ofMinutes(1)), Clock.systemUTC());
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String key = i % 2 == 0 ? "client-a" : "client-b";
            results.add(pool.submit(() -> {
                start.await();
                return realTimeLimiter.tryAcquire(key).allowed();
            }));
        }
        start.countDown();

        int allowed = 0;
        for (Future<Boolean> result : results) {
            allowed += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(allowed).isEqualTo(20); // exactly 10 for each of the two keys
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
