package com.meridian.disputes.application;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable rule violations, returned as RFC 9457 problem details.
 * <p>
 * Why this matters for an AI-native system: from Part 2 onward an LLM reads these
 * errors as tool results. "FILING_WINDOW_EXPIRED" plus a clear message lets the model
 * explain the outcome to the cardholder; a bare HTTP 500 makes it guess — or retry.
 * Error design IS tool design (Part 4).
 */
public enum ErrorCode {
    CARDHOLDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND),
    DISPUTE_NOT_FOUND(HttpStatus.NOT_FOUND),

    NOT_A_PURCHASE(HttpStatus.UNPROCESSABLE_CONTENT),
    TRANSACTION_NOT_POSTED(HttpStatus.UNPROCESSABLE_CONTENT),
    FILING_WINDOW_EXPIRED(HttpStatus.UNPROCESSABLE_CONTENT),
    ALREADY_REFUNDED(HttpStatus.UNPROCESSABLE_CONTENT),
    AMOUNT_EXCEEDS_DISPUTABLE(HttpStatus.UNPROCESSABLE_CONTENT),
    MISSING_REQUIRED_EVIDENCE(HttpStatus.UNPROCESSABLE_CONTENT),
    DUPLICATE_NOT_VERIFIED(HttpStatus.UNPROCESSABLE_CONTENT),
    DELIVERY_NOT_YET_DUE(HttpStatus.UNPROCESSABLE_CONTENT),
    NOT_RECURRING(HttpStatus.UNPROCESSABLE_CONTENT),
    CHARGED_BEFORE_CANCELLATION(HttpStatus.UNPROCESSABLE_CONTENT),
    CREDIT_EXCEEDS_DISPUTED(HttpStatus.UNPROCESSABLE_CONTENT),

    DISPUTE_ALREADY_ACTIVE(HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) { this.status = status; }

    public HttpStatus status() { return status; }
}
