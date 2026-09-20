package com.meridian.disputes.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(String id,
                          String cardId,
                          String cardholderId,
                          Merchant merchant,
                          Type type,
                          Status status,
                          BigDecimal amount,
                          String currency,
                          boolean recurring,
                          String originalTransactionId,
                          Instant authorizedAt,
                          Instant postedAt) {

    public enum Type { PURCHASE, REFUND }

    public enum Status { PENDING, POSTED, REVERSED }

    public boolean isPostedPurchase() {
        return type == Type.PURCHASE && status == Status.POSTED;
    }
}
