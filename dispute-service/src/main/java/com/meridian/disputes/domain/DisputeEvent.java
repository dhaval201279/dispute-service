package com.meridian.disputes.domain;

import java.time.Instant;

public record DisputeEvent(long id,
                           String disputeId,
                           String eventType,
                           String detail,
                           String actor,
                           Instant occurredAt) {
}
