package com.meridian.disputes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A dispute case. This record — not any chat transcript — is the system of record.
 * (Part 6 makes that distinction explicit: agent memory is a cache of the conversation,
 * never the source of truth for the case.)
 */
public record Dispute(String id,
                      String transactionId,
                      String cardholderId,
                      ReasonCode reasonCode,
                      DisputeStatus status,
                      Queue queue,
                      BigDecimal disputedAmount,
                      String currency,
                      String cardholderStatement,
                      String duplicateOfTransactionId,
                      LocalDate cancellationDate,
                      LocalDate expectedDeliveryDate,
                      LocalDate dueBy,
                      Instant createdAt,
                      Instant updatedAt,
                      int version) {
}
