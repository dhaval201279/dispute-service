package com.meridian.disputes.persistence;

import com.meridian.disputes.domain.Dispute;
import com.meridian.disputes.domain.DisputeEvent;
import com.meridian.disputes.domain.DisputeStatus;
import com.meridian.disputes.domain.ProvisionalCredit;
import com.meridian.disputes.domain.Queue;
import com.meridian.disputes.domain.ReasonCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DisputeRepository {

    private static final RowMapper<Dispute> DISPUTE = (rs, n) -> new Dispute(
            rs.getString("id"),
            rs.getString("transaction_id"),
            rs.getString("cardholder_id"),
            ReasonCode.fromCode(rs.getString("reason_code")),
            DisputeStatus.valueOf(rs.getString("status")),
            Queue.valueOf(rs.getString("queue")),
            rs.getBigDecimal("disputed_amount"),
            rs.getString("currency"),
            rs.getString("cardholder_statement"),
            rs.getString("duplicate_of_transaction_id"),
            Sql.date(rs, "cancellation_date"),
            Sql.date(rs, "expected_delivery_date"),
            Sql.date(rs, "due_by"),
            Sql.instant(rs, "created_at"),
            Sql.instant(rs, "updated_at"),
            rs.getInt("version"));

    private final JdbcClient jdbc;

    public DisputeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String nextId() {
        Long n = jdbc.sql("SELECT nextval('dispute_seq')").query(Long.class).single();
        return "DSP-" + n;
    }

    public void insert(Dispute d) {
        jdbc.sql("""
                        INSERT INTO disputes (id, transaction_id, cardholder_id, reason_code, status, queue,
                               disputed_amount, currency, cardholder_statement, duplicate_of_transaction_id,
                               cancellation_date, expected_delivery_date, due_by, created_at, updated_at, version)
                        VALUES (:id, :txn, :ch, :reason, :status, :queue, :amount, :currency, :statement,
                                :dupOf, :cancelled, :expectedDelivery, :dueBy, :createdAt, :updatedAt, 0)
                        """)
                .param("id", d.id())
                .param("txn", d.transactionId())
                .param("ch", d.cardholderId())
                .param("reason", d.reasonCode().code())
                .param("status", d.status().name())
                .param("queue", d.queue().name())
                .param("amount", d.disputedAmount())
                .param("currency", d.currency())
                .param("statement", d.cardholderStatement())
                .param("dupOf", d.duplicateOfTransactionId())
                .param("cancelled", d.cancellationDate())
                .param("expectedDelivery", d.expectedDeliveryDate())
                .param("dueBy", d.dueBy())
                .param("createdAt", Sql.ts(d.createdAt()))
                .param("updatedAt", Sql.ts(d.updatedAt()))
                .update();
    }

    public Optional<Dispute> findById(String id) {
        return jdbc.sql("SELECT * FROM disputes WHERE id = :id")
                .param("id", id)
                .query(DISPUTE)
                .optional();
    }

    public List<Dispute> findByCardholder(String cardholderId) {
        return jdbc.sql("SELECT * FROM disputes WHERE cardholder_id = :id ORDER BY created_at DESC")
                .param("id", cardholderId)
                .query(DISPUTE)
                .list();
    }

    public boolean hasActiveDispute(String transactionId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM disputes
                                        WHERE transaction_id = :txn
                                          AND status IN ('OPEN','UNDER_REVIEW','CHARGEBACK_FILED'))
                        """)
                .param("txn", transactionId)
                .query(Boolean.class)
                .single();
    }

    /** Optimistic-locked status change. */
    public void updateStatus(Dispute d, DisputeStatus newStatus, Instant now) {
        int rows = jdbc.sql("""
                        UPDATE disputes SET status = :status, updated_at = :now, version = version + 1
                         WHERE id = :id AND version = :version
                        """)
                .param("status", newStatus.name())
                .param("now", Sql.ts(now))
                .param("id", d.id())
                .param("version", d.version())
                .update();
        if (rows == 0) {
            throw new OptimisticLockingFailureException("Dispute " + d.id() + " was modified concurrently");
        }
    }

    // ------------------------------------------------------------------ events

    public void appendEvent(String disputeId, String type, String detail, String actor, Instant at) {
        jdbc.sql("""
                        INSERT INTO dispute_events (dispute_id, event_type, detail, actor, occurred_at)
                        VALUES (:id, :type, :detail, :actor, :at)
                        """)
                .param("id", disputeId)
                .param("type", type)
                .param("detail", detail)
                .param("actor", actor)
                .param("at", Sql.ts(at))
                .update();
    }

    public List<DisputeEvent> findEvents(String disputeId) {
        return jdbc.sql("SELECT * FROM dispute_events WHERE dispute_id = :id ORDER BY occurred_at, id")
                .param("id", disputeId)
                .query((rs, n) -> new DisputeEvent(
                        rs.getLong("id"), rs.getString("dispute_id"), rs.getString("event_type"),
                        rs.getString("detail"), rs.getString("actor"), Sql.instant(rs, "occurred_at")))
                .list();
    }

    // ------------------------------------------------------------------ credits

    public void insertCredit(String disputeId, BigDecimal amount, String currency, String issuedBy, Instant at) {
        jdbc.sql("""
                        INSERT INTO provisional_credits (dispute_id, amount, currency, issued_by, issued_at)
                        VALUES (:id, :amount, :currency, :by, :at)
                        """)
                .param("id", disputeId)
                .param("amount", amount)
                .param("currency", currency)
                .param("by", issuedBy)
                .param("at", Sql.ts(at))
                .update();
    }

    public List<ProvisionalCredit> findCredits(String disputeId) {
        return jdbc.sql("SELECT * FROM provisional_credits WHERE dispute_id = :id ORDER BY issued_at, id")
                .param("id", disputeId)
                .query((rs, n) -> new ProvisionalCredit(
                        rs.getLong("id"), rs.getString("dispute_id"), rs.getBigDecimal("amount"),
                        rs.getString("currency"), rs.getString("issued_by"), Sql.instant(rs, "issued_at")))
                .list();
    }

    public BigDecimal sumCredits(String disputeId) {
        return jdbc.sql("SELECT COALESCE(SUM(amount), 0) FROM provisional_credits WHERE dispute_id = :id")
                .param("id", disputeId)
                .query(BigDecimal.class)
                .single();
    }
}
