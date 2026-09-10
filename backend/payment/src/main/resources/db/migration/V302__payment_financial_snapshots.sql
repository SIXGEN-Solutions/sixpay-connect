-- PAYMENT_COMPLETION LOT 2.4
-- Durable reduced Payment financial snapshots.
-- These tables persist SIXPAY-owned facts only; they do not reproduce the
-- historical Amplitude bkeve/bkmvti physical schema.

CREATE TABLE payment_financial_event_snapshots
(
    snapshot_id                 UUID           NOT NULL,
    payment_id                  UUID           NOT NULL,
    public_payment_reference    VARCHAR(30)    NOT NULL,
    financial_institution_code  VARCHAR(32)    NOT NULL,
    debtor_account_reference    VARCHAR(256)   NOT NULL,
    creditor_account_reference  VARCHAR(256)   NOT NULL,
    requested_amount            NUMERIC(38,18) NOT NULL,
    requested_currency          VARCHAR(3)     NOT NULL,
    snapshot_version            VARCHAR(32)    NOT NULL,
    snapshot_status             VARCHAR(16)    NOT NULL,
    created_at                  TIMESTAMPTZ    NOT NULL,
    finalized_at                TIMESTAMPTZ,

    CONSTRAINT pk_payment_financial_event_snapshots
        PRIMARY KEY (snapshot_id),

    CONSTRAINT fk_payment_fin_event_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments (payment_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_payment_fin_event_payment
        UNIQUE (payment_id),

    CONSTRAINT ck_payment_fin_event_public_reference
        CHECK (
            public_payment_reference
                ~ '^PAY-[0-9A-HJKMNP-TV-Z]{26}$'
        ),

    CONSTRAINT ck_payment_fin_event_institution
        CHECK (
            financial_institution_code
                ~ '^[A-Z0-9][A-Z0-9_-]{1,31}$'
        ),

    CONSTRAINT ck_payment_fin_event_debtor_reference
        CHECK (btrim(debtor_account_reference) <> ''),

    CONSTRAINT ck_payment_fin_event_creditor_reference
        CHECK (btrim(creditor_account_reference) <> ''),

    CONSTRAINT ck_payment_fin_event_amount_positive
        CHECK (requested_amount > 0),

    CONSTRAINT ck_payment_fin_event_currency
        CHECK (requested_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_payment_fin_event_snapshot_version
        CHECK (btrim(snapshot_version) <> ''),

    CONSTRAINT ck_payment_fin_event_status
        CHECK (snapshot_status IN ('DRAFT', 'FINALIZED')),

    CONSTRAINT ck_payment_fin_event_finalization
        CHECK (
            (
                snapshot_status = 'DRAFT'
                AND finalized_at IS NULL
            )
            OR
            (
                snapshot_status = 'FINALIZED'
                AND finalized_at IS NOT NULL
                AND finalized_at >= created_at
            )
        )
);

CREATE INDEX idx_payment_fin_event_public_reference
    ON payment_financial_event_snapshots
        (public_payment_reference);

CREATE TABLE payment_financial_entry_snapshots
(
    entry_snapshot_id   UUID           NOT NULL,
    event_snapshot_id   UUID           NOT NULL,
    entry_sequence      INTEGER        NOT NULL,
    direction           VARCHAR(16)    NOT NULL,
    account_reference   VARCHAR(256)   NOT NULL,
    amount              NUMERIC(38,18) NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_payment_financial_entry_snapshots
        PRIMARY KEY (entry_snapshot_id),

    CONSTRAINT fk_payment_fin_entry_event
        FOREIGN KEY (event_snapshot_id)
        REFERENCES payment_financial_event_snapshots
            (snapshot_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT uk_payment_fin_entry_event_sequence
        UNIQUE (event_snapshot_id, entry_sequence),

    CONSTRAINT ck_payment_fin_entry_sequence
        CHECK (entry_sequence > 0),

    CONSTRAINT ck_payment_fin_entry_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),

    CONSTRAINT ck_payment_fin_entry_account_reference
        CHECK (btrim(account_reference) <> ''),

    CONSTRAINT ck_payment_fin_entry_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_payment_fin_entry_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_payment_fin_entry_event
    ON payment_financial_entry_snapshots
        (event_snapshot_id);

COMMENT ON TABLE payment_financial_event_snapshots IS
    'Payment-owned immutable reduced financial-event snapshot. Not an Amplitude bkeve persistence model.';

COMMENT ON TABLE payment_financial_entry_snapshots IS
    'Payment-owned immutable reduced financial-entry facts. Not an Amplitude bkmvti persistence model.';
