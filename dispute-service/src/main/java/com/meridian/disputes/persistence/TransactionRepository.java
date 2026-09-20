package com.meridian.disputes.persistence;

import com.meridian.disputes.domain.Merchant;
import com.meridian.disputes.domain.Transaction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private static final String SELECT = """
            SELECT t.id, t.card_id, c.cardholder_id, t.type, t.status, t.amount, t.currency,
                   t.recurring, t.original_transaction_id, t.authorized_at, t.posted_at,
                   m.id AS m_id, m.name AS m_name, m.statement_descriptor AS m_descriptor,
                   m.mcc AS m_mcc, m.category AS m_category, m.country AS m_country
              FROM transactions t
              JOIN cards c     ON c.id = t.card_id
              JOIN merchants m ON m.id = t.merchant_id
            """;

    private static final RowMapper<Transaction> MAPPER = (rs, n) -> new Transaction(
            rs.getString("id"),
            rs.getString("card_id"),
            rs.getString("cardholder_id"),
            new Merchant(rs.getString("m_id"), rs.getString("m_name"), rs.getString("m_descriptor"),
                    rs.getString("m_mcc"), rs.getString("m_category"), rs.getString("m_country")),
            Transaction.Type.valueOf(rs.getString("type")),
            Transaction.Status.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getBoolean("recurring"),
            rs.getString("original_transaction_id"),
            Sql.instant(rs, "authorized_at"),
            Sql.instant(rs, "posted_at"));

    private final JdbcClient jdbc;

    public TransactionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Transaction> findById(String id) {
        return jdbc.sql(SELECT + " WHERE t.id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }

    /** All transactions (purchases and refunds) for a cardholder authorised since {@code since}, newest first. */
    public List<Transaction> findByCardholderSince(String cardholderId, Instant since) {
        return jdbc.sql(SELECT + """
                         WHERE c.cardholder_id = :cardholderId
                           AND t.authorized_at >= :since
                         ORDER BY t.authorized_at DESC
                        """)
                .param("cardholderId", cardholderId)
                .param("since", Sql.ts(since))
                .query(MAPPER)
                .list();
    }

    /** Sum of POSTED merchant refunds against a purchase. */
    public BigDecimal sumPostedRefunds(String purchaseTransactionId) {
        return jdbc.sql("""
                        SELECT COALESCE(SUM(amount), 0) FROM transactions
                         WHERE original_transaction_id = :id AND type = 'REFUND' AND status = 'POSTED'
                        """)
                .param("id", purchaseTransactionId)
                .query(BigDecimal.class)
                .single();
    }
}
