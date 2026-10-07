package com.interviewprep.ratelimit;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // A fresh key per test keeps tests independent of the shared limiter state.
    private final String apiKey = "test-" + UUID.randomUUID();

    @Test
    void eleventhRequestInAMinuteIsRejected() throws Exception {
        for (int i = 1; i <= 10; i++) {
            getQuote(apiKey)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").exists())
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(10 - i)));
        }

        getQuote(apiKey)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.retryAfterSeconds", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(60))));
    }

    @Test
    void differentApiKeysDoNotAffectEachOther() throws Exception {
        for (int i = 0; i < 10; i++) {
            getQuote(apiKey).andExpect(status().isOk());
        }
        getQuote(apiKey).andExpect(status().isTooManyRequests());

        getQuote("other-" + UUID.randomUUID()).andExpect(status().isOk());
    }

    @Test
    void missingApiKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/quotes/random"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Missing X-API-Key header"));
        getQuote("  ").andExpect(status().isUnauthorized());
    }

    private ResultActions getQuote(String key) throws Exception {
        return mockMvc.perform(get("/api/quotes/random").header(RateLimitInterceptor.API_KEY_HEADER, key));
    }
}
