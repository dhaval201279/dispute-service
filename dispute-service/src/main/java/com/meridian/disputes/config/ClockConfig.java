package com.meridian.disputes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Injected clock: filing windows and SLAs must be testable without sleeping. */
@Configuration
class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
