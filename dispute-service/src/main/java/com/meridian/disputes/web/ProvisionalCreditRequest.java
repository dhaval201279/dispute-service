package com.meridian.disputes.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProvisionalCreditRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {
}
