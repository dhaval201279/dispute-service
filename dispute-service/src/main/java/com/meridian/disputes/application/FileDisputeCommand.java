package com.meridian.disputes.application;

import com.meridian.disputes.domain.ReasonCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything the legacy web form demands up front. Note what it asks of a stressed
 * cardholder: pick the right reason code, and for a duplicate, find and type the ID of
 * the OTHER transaction. The agent's job (Parts 1–3) is to gather this conversationally.
 */
public record FileDisputeCommand(String cardholderId,
                                 String transactionId,
                                 ReasonCode reasonCode,
                                 BigDecimal disputedAmount,          // null = full disputable amount
                                 String cardholderStatement,
                                 String duplicateOfTransactionId,    // DR-101
                                 LocalDate expectedDeliveryDate,     // DR-104
                                 LocalDate cancellationDate,         // DR-107
                                 String actor) {
}
