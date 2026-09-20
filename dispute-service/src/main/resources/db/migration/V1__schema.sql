-- DisputeDesk :: dispute-service schema (Part 0)
-- Readable, prefixed IDs are deliberate: from Part 2 onward an LLM reads and
-- emits these IDs. "TXN-100103" is far harder to hallucinate or transpose
-- than a UUID, and far easier to spot in a trace.

CREATE TABLE cardholders (
    id                 VARCHAR(20)  PRIMARY KEY,
    full_name          VARCHAR(120) NOT NULL,
    email              VARCHAR(160) NOT NULL UNIQUE,
    segment            VARCHAR(20)  NOT NULL,           -- STANDARD | PREMIUM
    preferred_language VARCHAR(10)  NOT NULL DEFAULT 'en',
    customer_since     DATE         NOT NULL
);

CREATE TABLE cards (
    id            VARCHAR(20) PRIMARY KEY,
    cardholder_id VARCHAR(20) NOT NULL REFERENCES cardholders (id),
    last4         CHAR(4)     NOT NULL,
    product       VARCHAR(40) NOT NULL,
    status        VARCHAR(20) NOT NULL                  -- ACTIVE | BLOCKED
);

CREATE TABLE merchants (
    id                   VARCHAR(20)  PRIMARY KEY,
    name                 VARCHAR(120) NOT NULL,
    statement_descriptor VARCHAR(40)  NOT NULL,         -- what the cardholder actually sees
    mcc                  CHAR(4)      NOT NULL,
    category             VARCHAR(60)  NOT NULL,
    country              CHAR(2)      NOT NULL
);

CREATE TABLE transactions (
    id                      VARCHAR(24)   PRIMARY KEY,
    card_id                 VARCHAR(20)   NOT NULL REFERENCES cards (id),
    merchant_id             VARCHAR(20)   NOT NULL REFERENCES merchants (id),
    type                    VARCHAR(10)   NOT NULL,     -- PURCHASE | REFUND
    status                  VARCHAR(12)   NOT NULL,     -- PENDING | POSTED | REVERSED
    amount                  NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency                CHAR(3)       NOT NULL,
    recurring               BOOLEAN       NOT NULL DEFAULT FALSE,
    original_transaction_id VARCHAR(24)   REFERENCES transactions (id),  -- set on REFUNDs
    authorized_at           TIMESTAMPTZ   NOT NULL,
    posted_at               TIMESTAMPTZ
);
CREATE INDEX ix_txn_card_auth ON transactions (card_id, authorized_at DESC);
CREATE INDEX ix_txn_original  ON transactions (original_transaction_id);

CREATE SEQUENCE dispute_seq START WITH 10001;

CREATE TABLE disputes (
    id                         VARCHAR(20)   PRIMARY KEY,
    transaction_id             VARCHAR(24)   NOT NULL REFERENCES transactions (id),
    cardholder_id              VARCHAR(20)   NOT NULL REFERENCES cardholders (id),
    reason_code                VARCHAR(10)   NOT NULL,  -- DR-101 | DR-104 | DR-107 | DR-201
    status                     VARCHAR(32)   NOT NULL,
    queue                      VARCHAR(20)   NOT NULL,  -- DISPUTES | FRAUD
    disputed_amount            NUMERIC(12,2) NOT NULL CHECK (disputed_amount > 0),
    currency                   CHAR(3)       NOT NULL,
    cardholder_statement       TEXT          NOT NULL,
    -- Reason-specific evidence. Explicit nullable columns, not JSON:
    -- the legacy form's rigidity is the point of Part 0.
    duplicate_of_transaction_id VARCHAR(24)  REFERENCES transactions (id),
    cancellation_date          DATE,
    expected_delivery_date     DATE,
    due_by                     DATE          NOT NULL,
    created_at                 TIMESTAMPTZ   NOT NULL,
    updated_at                 TIMESTAMPTZ   NOT NULL,
    version                    INT           NOT NULL DEFAULT 0
);
-- At most one active dispute per transaction, enforced by the database, not just code.
CREATE UNIQUE INDEX ux_one_active_dispute_per_txn
    ON disputes (transaction_id)
    WHERE status IN ('OPEN', 'UNDER_REVIEW', 'CHARGEBACK_FILED');
CREATE INDEX ix_disputes_cardholder ON disputes (cardholder_id, created_at DESC);

-- Append-only audit trail. In Part 9 every agent action lands here too,
-- with actor = 'agent:<name>' — the flight recorder regulators ask for.
CREATE TABLE dispute_events (
    id          BIGSERIAL    PRIMARY KEY,
    dispute_id  VARCHAR(20)  NOT NULL REFERENCES disputes (id),
    event_type  VARCHAR(40)  NOT NULL,
    detail      TEXT,
    actor       VARCHAR(60)  NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_events_dispute ON dispute_events (dispute_id, occurred_at);

-- Ledger of provisional credits. NOTE: no idempotency key yet — Part 4 adds it.
CREATE TABLE provisional_credits (
    id         BIGSERIAL     PRIMARY KEY,
    dispute_id VARCHAR(20)   NOT NULL REFERENCES disputes (id),
    amount     NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency   CHAR(3)       NOT NULL,
    issued_by  VARCHAR(60)   NOT NULL,
    issued_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX ix_credits_dispute ON provisional_credits (dispute_id);
