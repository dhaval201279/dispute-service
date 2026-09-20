package com.meridian.disputes.domain;

import java.util.EnumSet;
import java.util.Set;

public enum DisputeStatus {
    OPEN,
    UNDER_REVIEW,              // fraud queue
    CHARGEBACK_FILED,
    RESOLVED_CARDHOLDER_FAVOUR,
    RESOLVED_MERCHANT_FAVOUR,
    WITHDRAWN;

    public static final Set<DisputeStatus> ACTIVE = EnumSet.of(OPEN, UNDER_REVIEW, CHARGEBACK_FILED);

    public boolean isActive() { return ACTIVE.contains(this); }
}
