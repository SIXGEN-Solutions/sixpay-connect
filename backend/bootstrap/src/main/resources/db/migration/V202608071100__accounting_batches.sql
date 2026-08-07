CREATE TABLE IF NOT EXISTS accounting_batches (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    financial_institution_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounting_batches_idempotency_key
        UNIQUE (idempotency_key),
    CONSTRAINT ck_accounting_batches_status
        CHECK (status IN ('COMPLETED', 'NOT_COMPLETED'))
);

CREATE TABLE IF NOT EXISTS accounting_batch_items (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    public_payment_reference VARCHAR(128) NOT NULL,
    partner_id VARCHAR(128) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_occurred_at TIMESTAMPTZ NOT NULL,
    payment_business_date DATE NOT NULL,
    bank_posting_reference VARCHAR(128),
    tresorpay_status VARCHAR(64) NOT NULL,
    tresorpay_status_checked_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_accounting_batch_items_batch
        FOREIGN KEY (batch_id)
        REFERENCES accounting_batches(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_accounting_batch_items_payment_id
        UNIQUE (payment_id),
    CONSTRAINT ck_accounting_batch_items_amount
        CHECK (amount > 0),
    CONSTRAINT ck_accounting_batch_items_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounting_batch_items_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'REJECTED',
                'RECONCILIATION_REQUIRED'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_accounting_batches_business_date
    ON accounting_batches (
        business_date,
        financial_institution_code
    );

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_batch_id
    ON accounting_batch_items (batch_id);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_status
    ON accounting_batch_items (status);
