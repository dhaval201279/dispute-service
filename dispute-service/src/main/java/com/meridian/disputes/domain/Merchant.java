package com.meridian.disputes.domain;

/**
 * {@code statementDescriptor} is what appears on the cardholder's statement.
 * It is frequently cryptic ("SQ *TPR HSPTLTY"), which drives a large share of
 * real-world "I don't recognise this charge" disputes.
 */
public record Merchant(String id,
                       String name,
                       String statementDescriptor,
                       String mcc,
                       String category,
                       String country) {
}
