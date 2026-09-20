package com.meridian.disputes.web;

import com.meridian.disputes.domain.ReasonCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The legacy form, as JSON. Every field here is something the cardholder had to
 * know or look up themselves.
 */
public record CreateDisputeRequest(
        @NotBlank String cardholderId,
        @NotBlank String transactionId,
        @NotNull ReasonCode reasonCode,
        @DecimalMin(value = "0.01") BigDecimal disputedAmount,
        @NotBlank @Size(min = 10, max = 2000) String statement,
        String duplicateOfTransactionId,
        LocalDate expectedDeliveryDate,
        LocalDate cancellationDate) {
}
