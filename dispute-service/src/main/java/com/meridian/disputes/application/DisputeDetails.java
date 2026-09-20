package com.meridian.disputes.application;

import com.meridian.disputes.domain.Dispute;
import com.meridian.disputes.domain.DisputeEvent;
import com.meridian.disputes.domain.ProvisionalCredit;

import java.math.BigDecimal;
import java.util.List;

/** A dispute with its audit trail and credit ledger. */
public record DisputeDetails(Dispute dispute,
                             BigDecimal provisionalCreditTotal,
                             List<ProvisionalCredit> credits,
                             List<DisputeEvent> events) {
}
