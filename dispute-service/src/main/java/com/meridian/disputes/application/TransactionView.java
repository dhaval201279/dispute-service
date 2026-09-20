package com.meridian.disputes.application;

import com.meridian.disputes.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/** Read model: a transaction plus what has already been refunded against it. */
public record TransactionView(String id,
                              String type,
                              String status,
                              BigDecimal amount,
                              String currency,
                              BigDecimal refundedAmount,
                              boolean recurring,
                              String merchantName,
                              String statementDescriptor,
                              String merchantCategory,
                              String merchantCountry,
                              String originalTransactionId,
                              Instant authorizedAt,
                              Instant postedAt) {

    public static TransactionView of(Transaction t, BigDecimal refunded) {
        return new TransactionView(t.id(), t.type().name(), t.status().name(), t.amount(), t.currency(),
                refunded, t.recurring(), t.merchant().name(), t.merchant().statementDescriptor(),
                t.merchant().category(), t.merchant().country(), t.originalTransactionId(),
                t.authorizedAt(), t.postedAt());
    }
}
