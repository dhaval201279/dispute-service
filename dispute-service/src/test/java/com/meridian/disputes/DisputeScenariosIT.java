package com.meridian.disputes;

import com.meridian.disputes.application.DisputeRuleException;
import com.meridian.disputes.application.DisputeService;
import com.meridian.disputes.application.ErrorCode;
import com.meridian.disputes.application.FileDisputeCommand;
import com.meridian.disputes.domain.Dispute;
import com.meridian.disputes.domain.DisputeStatus;
import com.meridian.disputes.domain.Queue;
import com.meridian.disputes.domain.ReasonCode;
import com.meridian.disputes.domain.Resolution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * One test per seeded scenario (see docs/scenarios.md). These same scenarios become
 * the agent's eval cases in Part 8 — the deterministic system defines ground truth.
 * <p>
 * Each test runs in a rolled-back transaction, so seed data stays pristine.
 */
@SpringBootTest
@Testcontainers
@Transactional
class DisputeScenariosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @Autowired
    DisputeService service;

    private static final LocalDate TODAY = LocalDate.now();

    private static FileDisputeCommand cmd(String ch, String txn, ReasonCode reason) {
        return new FileDisputeCommand(ch, txn, reason, null, "Test statement from cardholder",
                null, null, null, "test");
    }

    @Test
    void s1_duplicateCharge_isAcceptedWhenOtherChargeIsVerified() {
        var c = new FileDisputeCommand("CH-1001", "TXN-100103", ReasonCode.DUPLICATE_CHARGE, null,
                "Charged twice for the same lamp", "TXN-100102", null, null, "test");

        Dispute d = service.fileDispute(c);

        assertThat(d.status()).isEqualTo(DisputeStatus.OPEN);
        assertThat(d.disputedAmount()).isEqualByComparingTo("4999.00");
        assertThat(d.id()).startsWith("DSP-");
    }

    @Test
    void s1_duplicateCharge_isRejectedWithoutTheOtherTransactionId() {
        assertThatThrownBy(() -> service.fileDispute(cmd("CH-1001", "TXN-100103", ReasonCode.DUPLICATE_CHARGE)))
                .isInstanceOf(DisputeRuleException.class)
                .extracting(e -> ((DisputeRuleException) e).code())
                .isEqualTo(ErrorCode.MISSING_REQUIRED_EVIDENCE);
    }

    @Test
    void s1_duplicateCharge_isRejectedForUnrelatedTransactions() {
        var c = new FileDisputeCommand("CH-1001", "TXN-100103", ReasonCode.DUPLICATE_CHARGE, null,
                "Charged twice", "TXN-100101", null, null, "test");
        assertCode(c, ErrorCode.DUPLICATE_NOT_VERIFIED);
    }

    @Test
    void s2_cancelledRecurring_isAcceptedWhenChargedAfterCancellation() {
        var c = new FileDisputeCommand("CH-1001", "TXN-100106", ReasonCode.CANCELLED_RECURRING, null,
                "I cancelled StreamFlix over a month ago", null, null, TODAY.minusDays(40), "test");
        assertThat(service.fileDispute(c).status()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void s2_cancelledRecurring_isRejectedWhenChargedBeforeCancellation() {
        var c = new FileDisputeCommand("CH-1001", "TXN-100104", ReasonCode.CANCELLED_RECURRING, null,
                "I cancelled StreamFlix", null, null, TODAY.minusDays(40), "test");
        assertCode(c, ErrorCode.CHARGED_BEFORE_CANCELLATION);
    }

    @Test
    void s3_outsideFilingWindow_isRejected() {
        var c = new FileDisputeCommand("CH-1001", "TXN-100107", ReasonCode.GOODS_NOT_RECEIVED, null,
                "Flight was cancelled", null, TODAY.minusDays(140), null, "test");
        assertCode(c, ErrorCode.FILING_WINDOW_EXPIRED);
    }

    @Test
    void s4_goodsNotReceived_isAccepted() {
        var c = new FileDisputeCommand("CH-1002", "TXN-100201", ReasonCode.GOODS_NOT_RECEIVED, null,
                "Order never arrived", null, TODAY.minusDays(15), null, "test");
        assertThat(service.fileDispute(c).disputedAmount()).isEqualByComparingTo("8750.00");
    }

    @Test
    void s5_alreadyRefunded_isRejected() {
        var c = new FileDisputeCommand("CH-1002", "TXN-100202", ReasonCode.GOODS_NOT_RECEIVED, null,
                "Order never arrived", null, TODAY.minusDays(30), null, "test");
        assertCode(c, ErrorCode.ALREADY_REFUNDED);
    }

    @Test
    void s6_pendingTransaction_isRejected() {
        assertCode(cmd("CH-1002", "TXN-100205", ReasonCode.UNRECOGNISED_FRAUD), ErrorCode.TRANSACTION_NOT_POSTED);
    }

    @Test
    void s8_fraud_isRoutedToFraudQueue() {
        Dispute d = service.fileDispute(cmd("CH-1003", "TXN-100302", ReasonCode.UNRECOGNISED_FRAUD));
        assertThat(d.queue()).isEqualTo(Queue.FRAUD);
        assertThat(d.status()).isEqualTo(DisputeStatus.UNDER_REVIEW);
    }

    @Test
    void cannotDisputeAnotherCardholdersTransaction() {
        assertCode(cmd("CH-1003", "TXN-100201", ReasonCode.UNRECOGNISED_FRAUD), ErrorCode.TRANSACTION_NOT_FOUND);
    }

    @Test
    void cannotOpenSecondActiveDisputeOnSameTransaction() {
        service.fileDispute(cmd("CH-1003", "TXN-100303", ReasonCode.UNRECOGNISED_FRAUD));
        assertCode(cmd("CH-1003", "TXN-100303", ReasonCode.UNRECOGNISED_FRAUD), ErrorCode.DISPUTE_ALREADY_ACTIVE);
    }

    @Test
    void provisionalCredit_isCappedAtDisputedAmount() {
        Dispute d = service.fileDispute(cmd("CH-1003", "TXN-100302", ReasonCode.UNRECOGNISED_FRAUD));
        assertThatThrownBy(() -> service.issueProvisionalCredit(d.id(), new BigDecimal("100.00"), "test"))
                .extracting(e -> ((DisputeRuleException) e).code())
                .isEqualTo(ErrorCode.CREDIT_EXCEEDS_DISPUTED);
    }

    /**
     * KNOWN ISSUE, documented as a test on purpose. A retried partial credit is applied twice.
     * Part 4 flips this assertion once the Idempotency-Key fix lands.
     */
    @Test
    void knownIssue_retriedPartialCreditIsAppliedTwice_fixedInPart4() {
        var c = new FileDisputeCommand("CH-1002", "TXN-100201", ReasonCode.GOODS_NOT_RECEIVED, null,
                "Order never arrived", null, TODAY.minusDays(15), null, "test");
        Dispute d = service.fileDispute(c);

        service.issueProvisionalCredit(d.id(), new BigDecimal("4375.00"), "analyst:test");
        var after = service.issueProvisionalCredit(d.id(), new BigDecimal("4375.00"), "analyst:test"); // "retry"

        assertThat(after.provisionalCreditTotal()).isEqualByComparingTo("8750.00"); // doubled, silently
    }

    @Test
    void merchantFavourResolution_flagsCreditReversal() {
        var c = new FileDisputeCommand("CH-1002", "TXN-100201", ReasonCode.GOODS_NOT_RECEIVED, null,
                "Order never arrived", null, TODAY.minusDays(15), null, "test");
        Dispute d = service.fileDispute(c);
        service.issueProvisionalCredit(d.id(), new BigDecimal("8750.00"), "analyst:test");

        var resolved = service.resolve(d.id(), Resolution.MERCHANT_FAVOUR, "Delivery proof provided", "analyst:test");

        assertThat(resolved.dispute().status()).isEqualTo(DisputeStatus.RESOLVED_MERCHANT_FAVOUR);
        assertThat(resolved.events()).anyMatch(e -> e.eventType().equals("PROVISIONAL_CREDIT_REVERSAL_REQUIRED"));
    }

    private void assertCode(FileDisputeCommand c, ErrorCode expected) {
        assertThatThrownBy(() -> service.fileDispute(c))
                .isInstanceOf(DisputeRuleException.class)
                .extracting(e -> ((DisputeRuleException) e).code())
                .isEqualTo(expected);
    }
}
