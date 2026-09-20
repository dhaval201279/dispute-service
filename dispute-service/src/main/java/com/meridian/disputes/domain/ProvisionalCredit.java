package com.meridian.disputes.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ProvisionalCredit(long id,
                                String disputeId,
                                BigDecimal amount,
                                String currency,
                                String issuedBy,
                                Instant issuedAt) {
}
