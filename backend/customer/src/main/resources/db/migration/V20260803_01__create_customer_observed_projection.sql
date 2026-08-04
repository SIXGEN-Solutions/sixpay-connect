CREATE TABLE customer_observed_customer (
    observed_customer_id UUID PRIMARY KEY,
    niu_protected VARCHAR(1024) NOT NULL,
    niu_search_hash CHAR(64) NOT NULL,
    legal_name_protected VARCHAR(2048) NOT NULL,
    legal_name_search_normalized VARCHAR(256) NOT NULL,
    phone_masked VARCHAR(128),
    email_masked VARCHAR(128),
    first_observed_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL,
    total_payments BIGINT NOT NULL,
    successful_payments BIGINT NOT NULL,
    failed_payments BIGINT NOT NULL,
    last_payment_status VARCHAR(64) NOT NULL,
    last_failure_reason_code VARCHAR(64),
    projection_version BIGINT NOT NULL,
    source_event_watermark VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_customer_observed_customer_niu_hash
        UNIQUE (niu_search_hash),
    CONSTRAINT ck_customer_observed_customer_total
        CHECK (total_payments >= 1),
    CONSTRAINT ck_customer_observed_customer_success
        CHECK (successful_payments >= 0),
    CONSTRAINT ck_customer_observed_customer_failed
        CHECK (failed_payments >= 0),
    CONSTRAINT ck_customer_observed_customer_counters
        CHECK (
            successful_payments + failed_payments
                <= total_payments
        ),
    CONSTRAINT ck_customer_observed_customer_version
        CHECK (projection_version >= 1),
    CONSTRAINT ck_customer_observed_customer_dates
        CHECK (
            last_observed_at >= first_observed_at
            AND updated_at >= last_observed_at
        )
);

CREATE INDEX ix_customer_observed_customer_name_search
    ON customer_observed_customer (
        legal_name_search_normalized
    );

CREATE INDEX ix_customer_observed_customer_last_observed
    ON customer_observed_customer (
        last_observed_at DESC
    );

CREATE TABLE customer_observed_institution (
    observed_institution_id BIGSERIAL PRIMARY KEY,
    observed_customer_id UUID NOT NULL,
    financial_institution_code VARCHAR(32) NOT NULL,
    first_observed_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_customer_observed_institution_customer
        FOREIGN KEY (observed_customer_id)
        REFERENCES customer_observed_customer (
            observed_customer_id
        )
        ON DELETE CASCADE,
    CONSTRAINT uk_customer_observed_institution
        UNIQUE (
            observed_customer_id,
            financial_institution_code
        ),
    CONSTRAINT ck_customer_observed_institution_dates
        CHECK (last_observed_at >= first_observed_at)
);

CREATE INDEX ix_customer_observed_institution_customer
    ON customer_observed_institution (
        observed_customer_id
    );

CREATE TABLE customer_observed_account (
    observed_account_id BIGSERIAL PRIMARY KEY,
    observed_institution_id BIGINT NOT NULL,
    account_binding_fingerprint VARCHAR(67) NOT NULL,
    masked_value VARCHAR(32) NOT NULL,

    CONSTRAINT fk_customer_observed_account_institution
        FOREIGN KEY (observed_institution_id)
        REFERENCES customer_observed_institution (
            observed_institution_id
        )
        ON DELETE CASCADE,
    CONSTRAINT uk_customer_observed_account
        UNIQUE (
            observed_institution_id,
            account_binding_fingerprint
        ),
    CONSTRAINT ck_customer_observed_account_fingerprint
        CHECK (
            account_binding_fingerprint
                ~ '^v1:[0-9a-f]{64}$'
        )
);

CREATE TABLE customer_observed_payment (
    payment_id UUID PRIMARY KEY,
    observed_customer_id UUID NOT NULL,
    public_payment_reference VARCHAR(128) NOT NULL,
    financial_institution_code VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    payment_status VARCHAR(64) NOT NULL,
    failure_reason_code VARCHAR(64),
    payment_created_at TIMESTAMPTZ NOT NULL,
    payment_updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_customer_observed_payment_customer
        FOREIGN KEY (observed_customer_id)
        REFERENCES customer_observed_customer (
            observed_customer_id
        )
        ON DELETE CASCADE,
    CONSTRAINT ck_customer_observed_payment_amount
        CHECK (amount >= 0),
    CONSTRAINT ck_customer_observed_payment_dates
        CHECK (
            payment_updated_at >= payment_created_at
        )
);

CREATE INDEX ix_customer_observed_payment_customer
    ON customer_observed_payment (
        observed_customer_id,
        payment_created_at DESC
    );

CREATE INDEX ix_customer_observed_payment_status
    ON customer_observed_payment (
        payment_status,
        payment_updated_at DESC
    );

CREATE TABLE customer_observation_processed_event (
    source_event_id UUID PRIMARY KEY,
    observed_customer_id UUID NOT NULL,
    source_event_watermark VARCHAR(256) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_customer_observation_event_customer
        FOREIGN KEY (observed_customer_id)
        REFERENCES customer_observed_customer (
            observed_customer_id
        )
        ON DELETE CASCADE
);

CREATE INDEX ix_customer_observation_event_customer
    ON customer_observation_processed_event (
        observed_customer_id,
        processed_at DESC
    );
