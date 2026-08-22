CREATE TABLE customer_management_customer (
    customer_id UUID PRIMARY KEY,
    financial_institution_code VARCHAR(32) NOT NULL,
    banking_customer_reference VARCHAR(100) NOT NULL,
    customer_number VARCHAR(100),
    niu VARCHAR(100),
    legal_name VARCHAR(200) NOT NULL,
    email VARCHAR(254),
    phone_number VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_customer_management_banking_identity
        UNIQUE (
            financial_institution_code,
            banking_customer_reference
        ),
    CONSTRAINT ck_customer_management_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ck_customer_management_status_reason
        CHECK (
            (status = 'ACTIVE' AND status_reason IS NULL)
            OR
            (status IN ('SUSPENDED', 'CLOSED')
                AND status_reason IS NOT NULL)
        ),
    CONSTRAINT ck_customer_management_dates
        CHECK (updated_at >= created_at),
    CONSTRAINT ck_customer_management_row_version
        CHECK (row_version >= 0)
);

CREATE INDEX ix_customer_management_customer_status
    ON customer_management_customer (status);

CREATE INDEX ix_customer_management_customer_number
    ON customer_management_customer (
        financial_institution_code,
        customer_number
    )
    WHERE customer_number IS NOT NULL;

CREATE INDEX ix_customer_management_customer_niu
    ON customer_management_customer (
        financial_institution_code,
        niu
    )
    WHERE niu IS NOT NULL;

CREATE TABLE customer_management_bank_account (
    bank_account_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    banking_account_reference VARCHAR(100) NOT NULL,
    account_binding_fingerprint VARCHAR(128) NOT NULL,
    masked_account_identifier VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    account_type VARCHAR(40),
    default_account BOOLEAN NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_customer_management_bank_account_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_management_customer (customer_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_customer_management_account_reference
        UNIQUE (
            customer_id,
            banking_account_reference
        ),
    CONSTRAINT uk_customer_management_account_fingerprint
        UNIQUE (
            customer_id,
            account_binding_fingerprint
        ),
    CONSTRAINT ck_customer_management_currency
        CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_customer_management_fingerprint
        CHECK (
            account_binding_fingerprint
                ~ '^v1:[0-9a-f]{64}$'
        )
);

CREATE UNIQUE INDEX uk_customer_management_default_account
    ON customer_management_bank_account (customer_id)
    WHERE default_account = TRUE;

CREATE INDEX ix_customer_management_bank_account_customer
    ON customer_management_bank_account (customer_id);
