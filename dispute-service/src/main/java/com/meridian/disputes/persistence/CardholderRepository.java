package com.meridian.disputes.persistence;

import com.meridian.disputes.domain.Cardholder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CardholderRepository {

    private final JdbcClient jdbc;

    public CardholderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Cardholder> findById(String id) {
        return jdbc.sql("""
                        SELECT id, full_name, email, segment, preferred_language, customer_since
                          FROM cardholders WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, n) -> new Cardholder(
                        rs.getString("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("segment"),
                        rs.getString("preferred_language"),
                        Sql.date(rs, "customer_since")))
                .optional();
    }
}
