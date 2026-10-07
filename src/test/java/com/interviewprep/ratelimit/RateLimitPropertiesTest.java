package com.interviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RateLimitPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

    @Test
    void bareNumberWindowMeansSeconds() {
        runner.withPropertyValues("app.rate-limit.requests=10", "app.rate-limit.window=60")
                .run(context -> assertThat(context.getBean(RateLimitProperties.class).window())
                        .isEqualTo(Duration.ofSeconds(60)));
    }

    @Test
    void invalidConfigurationStopsStartup() {
        runner.withPropertyValues("app.rate-limit.requests=0", "app.rate-limit.window=1m")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.rate-limit.requests=10", "app.rate-limit.window=0s")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.rate-limit.requests=10", "app.rate-limit.window=abc")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class PropertiesConfig {
    }
}
