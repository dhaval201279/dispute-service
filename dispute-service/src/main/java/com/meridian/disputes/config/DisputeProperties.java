package com.meridian.disputes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "disputes")
public record DisputeProperties(int resolutionSlaDays, int duplicateWindowHours) {
}
