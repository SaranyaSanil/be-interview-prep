package com.interviewprep.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies the rate limit before the controller runs. Exceptions thrown here still go through
 * Spring MVC's exception handling, so 401/429 use the same ProblemDetail format as other errors
 * (a servlet Filter would bypass it).
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final SlidingWindowRateLimiter rateLimiter;

    public RateLimitInterceptor(SlidingWindowRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            throw new ErrorResponseException(HttpStatus.UNAUTHORIZED,
                    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing " + API_KEY_HEADER + " header"),
                    null);
        }

        RateLimitDecision decision = rateLimiter.tryAcquire(apiKey.strip());
        if (!decision.allowed()) {
            throw tooManyRequests(decision.retryAfterSeconds());
        }
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        return true;
    }

    private static ErrorResponseException tooManyRequests(long retryAfterSeconds) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds");
        body.setProperty("retryAfterSeconds", retryAfterSeconds);
        ErrorResponseException exception = new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, body, null);
        exception.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        return exception;
    }
}
