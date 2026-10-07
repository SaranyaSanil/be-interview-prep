package com.interviewprep.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The limit and window come from configuration: here 3 requests per 10 seconds, no code change. */
@SpringBootTest(properties = {"app.rate-limit.requests=3", "app.rate-limit.window=10s"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredLimitAndWindowAreApplied() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/quotes/random").header("X-API-Key", "configured"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/quotes/random").header("X-API-Key", "configured"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "10"));
    }
}
