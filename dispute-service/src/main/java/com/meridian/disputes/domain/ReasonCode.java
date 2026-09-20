package com.meridian.disputes.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Fictional Meridian Bank dispute reason codes. Deliberately NOT real card-network codes.
 * <p>
 * In the legacy form the cardholder must pick one of these themselves. Most people
 * cannot tell "duplicate" from "cancelled recurring" from "fraud" — which is exactly
 * the gap the agent closes in Parts 1–2.
 */
public enum ReasonCode {

    DUPLICATE_CHARGE("DR-101", "Duplicate charge", 120, Queue.DISPUTES),
    GOODS_NOT_RECEIVED("DR-104", "Goods or services not received", 120, Queue.DISPUTES),
    CANCELLED_RECURRING("DR-107", "Charged after cancelling a recurring payment", 120, Queue.DISPUTES),
    UNRECOGNISED_FRAUD("DR-201", "Unrecognised or fraudulent transaction", 60, Queue.FRAUD);

    private final String code;
    private final String description;
    private final int filingWindowDays;
    private final Queue queue;

    ReasonCode(String code, String description, int filingWindowDays, Queue queue) {
        this.code = code;
        this.description = description;
        this.filingWindowDays = filingWindowDays;
        this.queue = queue;
    }

    @JsonValue
    public String code() { return code; }

    public String description() { return description; }

    public int filingWindowDays() { return filingWindowDays; }

    public Queue queue() { return queue; }

    @JsonCreator
    public static ReasonCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(rc -> rc.code.equalsIgnoreCase(code) || rc.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown reason code: " + code));
    }
}
