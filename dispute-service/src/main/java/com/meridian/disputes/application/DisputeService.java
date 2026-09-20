package com.meridian.disputes.application;

import com.meridian.disputes.config.DisputeProperties;
import com.meridian.disputes.domain.Dispute;
import com.meridian.disputes.domain.DisputeStatus;
import com.meridian.disputes.domain.Queue;
import com.meridian.disputes.domain.ReasonCode;
import com.meridian.disputes.domain.Resolution;
import com.meridian.disputes.domain.Transaction;
import com.meridian.disputes.persistence.DisputeRepository;
import com.meridian.disputes.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static com.meridian.disputes.application.ErrorCode.*;

/**
 * Meridian Bank's dispute rules — deterministic, auditable, boring. On purpose.
 * <p>
 * The central design principle of the whole series starts here:
 * <b>code constrains everything it can; the model decides only what is genuinely open.</b>
 * Filing windows, refund netting, duplicate verification and credit caps are NOT the
 * agent's job, and never will be. The agent's job is to turn "I think I got charged twice
 * at that furniture place last week" into a valid {@link FileDisputeCommand}. If it gets
 * that wrong, these rules reject it — no matter how confident the model sounds.
 */
@Service
public class DisputeService {

    private final DisputeRepository disputes;
    private final TransactionRepository transactions;
    private final DisputeProperties props;
    private final Clock clock;

    public DisputeService(DisputeRepository disputes, TransactionRepository transactions,
                          DisputeProperties props, Clock clock) {
        this.disputes = disputes;
        this.transactions = transactions;
        this.props = props;
        this.clock = clock;
    }

    // ================================================================== file

    @Transactional
    public Dispute fileDispute(FileDisputeCommand cmd) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        Transaction txn = transactions.findById(cmd.transactionId())
                .filter(t -> t.cardholderId().equals(cmd.cardholderId())) // don't reveal others' txns
                .orElseThrow(() -> new DisputeRuleException(TRANSACTION_NOT_FOUND,
                        "No transaction " + cmd.transactionId() + " for cardholder " + cmd.cardholderId()));

        if (txn.type() != Transaction.Type.PURCHASE) {
            throw new DisputeRuleException(NOT_A_PURCHASE,
                    txn.id() + " is a " + txn.type() + "; only purchases can be disputed");
        }
        if (txn.status() != Transaction.Status.POSTED) {
            throw new DisputeRuleException(TRANSACTION_NOT_POSTED,
                    txn.id() + " is " + txn.status() + "; wait until it posts before disputing");
        }

        ReasonCode reason = cmd.reasonCode();
        Instant windowCloses = txn.postedAt().plus(Duration.ofDays(reason.filingWindowDays()));
        if (now.isAfter(windowCloses)) {
            throw new DisputeRuleException(FILING_WINDOW_EXPIRED,
                    "%s must be filed within %d days of posting; %s posted on %s".formatted(
                            reason.code(), reason.filingWindowDays(), txn.id(),
                            LocalDate.ofInstant(txn.postedAt(), ZoneOffset.UTC)));
        }

        if (disputes.hasActiveDispute(txn.id())) {
            throw new DisputeRuleException(DISPUTE_ALREADY_ACTIVE,
                    txn.id() + " already has an active dispute");
        }

        BigDecimal refunded = transactions.sumPostedRefunds(txn.id());
        BigDecimal disputable = txn.amount().subtract(refunded);
        if (disputable.signum() <= 0) {
            throw new DisputeRuleException(ALREADY_REFUNDED,
                    "Merchant has already refunded %s %s against %s; nothing left to dispute"
                            .formatted(refunded, txn.currency(), txn.id()));
        }
        BigDecimal amount = cmd.disputedAmount() == null ? disputable : cmd.disputedAmount();
        if (amount.signum() <= 0 || amount.compareTo(disputable) > 0) {
            throw new DisputeRuleException(AMOUNT_EXCEEDS_DISPUTABLE,
                    "Disputed amount must be between 0 and %s %s (purchase %s minus refunds %s)"
                            .formatted(disputable, txn.currency(), txn.amount(), refunded));
        }

        validateReasonSpecificEvidence(cmd, txn, today);

        Dispute dispute = new Dispute(
                disputes.nextId(), txn.id(), cmd.cardholderId(), reason,
                reason.queue() == Queue.FRAUD
                        ? DisputeStatus.UNDER_REVIEW : DisputeStatus.OPEN,
                reason.queue(), amount, txn.currency(), cmd.cardholderStatement(),
                cmd.duplicateOfTransactionId(), cmd.cancellationDate(), cmd.expectedDeliveryDate(),
                today.plusDays(props.resolutionSlaDays()), now, now, 0);

        disputes.insert(dispute);
        disputes.appendEvent(dispute.id(), "CREATED",
                "%s for %s %s, routed to %s".formatted(reason.code(), amount, txn.currency(), reason.queue()),
                cmd.actor(), now);
        return dispute;
    }

    private void validateReasonSpecificEvidence(FileDisputeCommand cmd, Transaction txn, LocalDate today) {
        switch (cmd.reasonCode()) {
            case DUPLICATE_CHARGE -> verifyDuplicate(cmd, txn);
            case GOODS_NOT_RECEIVED -> {
                if (cmd.expectedDeliveryDate() == null) {
                    throw new DisputeRuleException(MISSING_REQUIRED_EVIDENCE,
                            "DR-104 requires expectedDeliveryDate");
                }
                if (!cmd.expectedDeliveryDate().isBefore(today)) {
                    throw new DisputeRuleException(DELIVERY_NOT_YET_DUE,
                            "Expected delivery date " + cmd.expectedDeliveryDate() + " has not passed yet");
                }
            }
            case CANCELLED_RECURRING -> {
                if (!txn.recurring()) {
                    throw new DisputeRuleException(NOT_RECURRING,
                            txn.id() + " is not a recurring payment; DR-107 does not apply");
                }
                if (cmd.cancellationDate() == null) {
                    throw new DisputeRuleException(MISSING_REQUIRED_EVIDENCE,
                            "DR-107 requires cancellationDate");
                }
                LocalDate charged = LocalDate.ofInstant(txn.authorizedAt(), ZoneOffset.UTC);
                if (!charged.isAfter(cmd.cancellationDate())) {
                    throw new DisputeRuleException(CHARGED_BEFORE_CANCELLATION,
                            "%s was charged on %s, before the stated cancellation on %s"
                                    .formatted(txn.id(), charged, cmd.cancellationDate()));
                }
            }
            case UNRECOGNISED_FRAUD -> {
                // No extra evidence at intake: fraud goes straight to the FRAUD queue.
            }
        }
    }

    private void verifyDuplicate(FileDisputeCommand cmd, Transaction txn) {
        String otherId = cmd.duplicateOfTransactionId();
        if (otherId == null || otherId.isBlank()) {
            throw new DisputeRuleException(MISSING_REQUIRED_EVIDENCE,
                    "DR-101 requires duplicateOfTransactionId (the other, original charge)");
        }
        Transaction other = transactions.findById(otherId)
                .filter(t -> t.cardholderId().equals(cmd.cardholderId()))
                .orElseThrow(() -> new DisputeRuleException(DUPLICATE_NOT_VERIFIED,
                        "No transaction " + otherId + " for this cardholder"));

        long hoursApart = Math.abs(Duration.between(txn.authorizedAt(), other.authorizedAt()).toHours());
        boolean verified = !other.id().equals(txn.id())
                && other.isPostedPurchase()
                && other.merchant().id().equals(txn.merchant().id())
                && other.amount().compareTo(txn.amount()) == 0
                && other.currency().equals(txn.currency())
                && hoursApart <= props.duplicateWindowHours();
        if (!verified) {
            throw new DisputeRuleException(DUPLICATE_NOT_VERIFIED,
                    "%s and %s are not a verifiable duplicate (same merchant, same amount, within %dh)"
                            .formatted(txn.id(), other.id(), props.duplicateWindowHours()));
        }
    }

    // ================================================================== actions on a dispute

    @Transactional(readOnly = true)
    public DisputeDetails details(String disputeId) {
        Dispute d = load(disputeId);
        return new DisputeDetails(d, disputes.sumCredits(d.id()), disputes.findCredits(d.id()),
                disputes.findEvents(d.id()));
    }

    /**
     * Issue a provisional credit to the cardholder while the dispute is investigated.
     * <p>
     * <b>KNOWN ISSUE — intentionally left in for Part 4.</b> This operation is NOT idempotent.
     * The cap below stops credits exceeding the disputed amount, but a retried PARTIAL credit
     * (network timeout, or an LLM that "tries again to be sure") silently doubles within the
     * cap. Part 4 reproduces this with an agent and fixes it with an Idempotency-Key.
     */
    @Transactional
    public DisputeDetails issueProvisionalCredit(String disputeId, BigDecimal amount, String actor) {
        Dispute d = load(disputeId);
        requireActive(d, "issue a provisional credit");
        BigDecimal alreadyCredited = disputes.sumCredits(d.id());
        if (amount.signum() <= 0 || alreadyCredited.add(amount).compareTo(d.disputedAmount()) > 0) {
            throw new DisputeRuleException(CREDIT_EXCEEDS_DISPUTED,
                    "Credit of %s would exceed disputed amount %s (already credited %s)"
                            .formatted(amount, d.disputedAmount(), alreadyCredited));
        }
        Instant now = clock.instant();
        disputes.insertCredit(d.id(), amount, d.currency(), actor, now);
        disputes.appendEvent(d.id(), "PROVISIONAL_CREDIT_ISSUED", amount + " " + d.currency(), actor, now);
        return details(d.id());
    }

    @Transactional
    public DisputeDetails fileChargeback(String disputeId, String actor) {
        Dispute d = load(disputeId);
        if (d.status() != DisputeStatus.OPEN) {
            throw new DisputeRuleException(INVALID_STATE_TRANSITION,
                    "Chargeback can only be filed on an OPEN dispute; " + d.id() + " is " + d.status());
        }
        Instant now = clock.instant();
        disputes.updateStatus(d, DisputeStatus.CHARGEBACK_FILED, now);
        disputes.appendEvent(d.id(), "CHARGEBACK_FILED", "Sent to acquirer for merchant response", actor, now);
        return details(d.id());
    }

    @Transactional
    public DisputeDetails resolve(String disputeId, Resolution resolution, String note, String actor) {
        Dispute d = load(disputeId);
        requireActive(d, "resolve");
        Instant now = clock.instant();
        disputes.updateStatus(d, resolution.resultingStatus(), now);
        disputes.appendEvent(d.id(), "RESOLVED", resolution + (note == null ? "" : ": " + note), actor, now);

        BigDecimal credited = disputes.sumCredits(d.id());
        if (resolution == Resolution.MERCHANT_FAVOUR && credited.signum() > 0) {
            // Provisional credit is reversible by design. Part 9 builds its whole
            // "reversible-with-approval" autonomy tier on this property.
            disputes.appendEvent(d.id(), "PROVISIONAL_CREDIT_REVERSAL_REQUIRED",
                    credited + " " + d.currency(), "system", now);
        }
        return details(d.id());
    }

    private Dispute load(String disputeId) {
        return disputes.findById(disputeId)
                .orElseThrow(() -> new DisputeRuleException(DISPUTE_NOT_FOUND, "No dispute " + disputeId));
    }

    private static void requireActive(Dispute d, String action) {
        if (!d.status().isActive()) {
            throw new DisputeRuleException(INVALID_STATE_TRANSITION,
                    "Cannot " + action + ": " + d.id() + " is " + d.status());
        }
    }
}
