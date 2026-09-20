package com.meridian.disputes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Meridian Bank's existing dispute system — the deterministic "before" picture.
 * <p>
 * Nothing in this module will ever call an LLM. From Part 2 onward, the AI agent
 * sits <em>in front of</em> this service and uses its REST API as tools. That is
 * the core architectural bet of the series: AI-native systems wrap and reuse the
 * enterprise core; they do not replace it.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DisputeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DisputeServiceApplication.class, args);
    }
}
