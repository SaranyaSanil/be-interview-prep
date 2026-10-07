package com.interviewprep.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A single injectable Clock so time-dependent rules can be tested with a fixed time. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
