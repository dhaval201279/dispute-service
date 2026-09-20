package com.meridian.disputes.domain;

public enum Resolution {
    CARDHOLDER_FAVOUR(DisputeStatus.RESOLVED_CARDHOLDER_FAVOUR),
    MERCHANT_FAVOUR(DisputeStatus.RESOLVED_MERCHANT_FAVOUR),
    WITHDRAWN(DisputeStatus.WITHDRAWN);

    private final DisputeStatus resultingStatus;

    Resolution(DisputeStatus resultingStatus) { this.resultingStatus = resultingStatus; }

    public DisputeStatus resultingStatus() { return resultingStatus; }
}
