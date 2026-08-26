-- SIXPAY CONNECT canonical pre-production Flyway baseline
-- FS-2.3 Database baseline consolidation
-- This file represents current schema state; Git preserves prior migration history.


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260803_01__create_customer_observed_projection.sql
-- ---------------------------------------------------------------------------

CREATE TABLE customer_observed_customer (
    observed_customer_id UUID PRIMARY KEY,
    niu_protected VARCHAR(1024) NOT NULL,
    niu_search_hash VARCHAR(64) NOT NULL,
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
    observed_institution_id UUID PRIMARY KEY,
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
    observed_account_id UUID PRIMARY KEY,
    observed_institution_id UUID NOT NULL,
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
    currency VARCHAR(3) NOT NULL,
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


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260804.01__add_observed_customer_query_indexes.sql
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_customer_last_observed_keyset
ON customer_observed_customer (
    last_observed_at DESC,
    observed_customer_id DESC
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_customer_first_observed_keyset
ON customer_observed_customer (
    first_observed_at DESC,
    observed_customer_id DESC
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_customer_snapshot
ON customer_observed_customer (
    updated_at,
    observed_customer_id
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_customer_status
ON customer_observed_customer (
    last_payment_status,
    last_failure_reason_code
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_customer_legal_name_prefix
ON customer_observed_customer (
    legal_name_search_normalized text_pattern_ops
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_institution_query
ON customer_observed_institution (
    financial_institution_code,
    observed_customer_id
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_payment_customer_keyset
ON customer_observed_payment (
    observed_customer_id,
    payment_created_at DESC,
    payment_id DESC
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_payment_time_filter
ON customer_observed_payment (
    payment_created_at,
    observed_customer_id
);

CREATE INDEX IF NOT EXISTS
    idx_customer_observed_payment_snapshot
ON customer_observed_payment (
    observed_customer_id,
    payment_updated_at
);


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260805.01__create_customer_observation_audit.sql
-- ---------------------------------------------------------------------------

CREATE TABLE customer_observation_audit (
    audit_id UUID PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    observed_customer_id UUID NULL,
    payment_id UUID NULL,
    source_event_id UUID NULL,
    actor_id VARCHAR(150) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    reason_code VARCHAR(100) NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    audit_version INTEGER NOT NULL,

    CONSTRAINT ck_customer_observation_audit_version
        CHECK (audit_version > 0),

    CONSTRAINT ck_customer_observation_audit_actor
        CHECK (length(btrim(actor_id)) > 0),

    CONSTRAINT ck_customer_observation_audit_correlation
        CHECK (length(btrim(correlation_id)) > 0),

    CONSTRAINT ck_customer_observation_audit_reason
        CHECK (
            reason_code IS NULL
            OR reason_code ~ '^[A-Z0-9][A-Z0-9_.-]*$'
        )
);

CREATE INDEX idx_customer_observation_audit_customer
    ON customer_observation_audit (
        observed_customer_id,
        occurred_at
    );

CREATE INDEX idx_customer_observation_audit_source_event
    ON customer_observation_audit (
        source_event_id
    );

CREATE INDEX idx_customer_observation_audit_correlation
    ON customer_observation_audit (
        correlation_id,
        occurred_at
    );

CREATE INDEX idx_customer_observation_audit_occurred
    ON customer_observation_audit (
        occurred_at
    );

CREATE OR REPLACE FUNCTION reject_customer_observation_audit_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'customer_observation_audit is append-only';
END;
$$;

CREATE TRIGGER trg_customer_observation_audit_no_update
BEFORE UPDATE ON customer_observation_audit
FOR EACH ROW
EXECUTE FUNCTION reject_customer_observation_audit_mutation();

CREATE TRIGGER trg_customer_observation_audit_no_delete
BEFORE DELETE ON customer_observation_audit
FOR EACH ROW
EXECUTE FUNCTION reject_customer_observation_audit_mutation();


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260822.01__create_customer_management.sql
-- ---------------------------------------------------------------------------

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


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260822.02__create_customer_subscription.sql
-- ---------------------------------------------------------------------------

CREATE TABLE customer_management_subscription (
    subscription_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    partner_id UUID NOT NULL,
    bank_account_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_customer_subscription_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_management_customer (customer_id),

    CONSTRAINT fk_customer_subscription_account
        FOREIGN KEY (bank_account_id)
        REFERENCES customer_management_bank_account (bank_account_id),

    CONSTRAINT ck_customer_subscription_status
        CHECK (
            status IN (
                'PENDING_ACTIVATION',
                'ACTIVE',
                'SUSPENDED',
                'CLOSED'
            )
        ),

    CONSTRAINT ck_customer_subscription_timeline
        CHECK (
            updated_at >= created_at
            AND (
                activated_at IS NULL
                OR activated_at >= created_at
            )
            AND (
                closed_at IS NULL
                OR closed_at >= created_at
            )
        ),

    CONSTRAINT ck_customer_subscription_state
        CHECK (
            (
                status = 'PENDING_ACTIVATION'
                AND status_reason IS NULL
                AND activated_at IS NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'ACTIVE'
                AND status_reason IS NULL
                AND activated_at IS NOT NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'SUSPENDED'
                AND status_reason IS NOT NULL
                AND activated_at IS NOT NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'CLOSED'
                AND status_reason IS NOT NULL
                AND closed_at IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uk_customer_subscription_open_partner
    ON customer_management_subscription (
        customer_id,
        partner_id
    )
    WHERE status <> 'CLOSED';

CREATE INDEX ix_customer_subscription_customer
    ON customer_management_subscription (
        customer_id,
        created_at DESC
    );

CREATE INDEX ix_customer_subscription_partner
    ON customer_management_subscription (
        partner_id,
        status
    );

CREATE INDEX ix_customer_subscription_account
    ON customer_management_subscription (
        bank_account_id
    );


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260822.03__link_observed_customer_to_customer.sql
-- ---------------------------------------------------------------------------

CREATE TABLE customer_observed_master_link (
    observed_customer_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    link_status VARCHAR(16) NOT NULL,

    linked_by VARCHAR(200) NOT NULL,
    link_correlation_id VARCHAR(150) NOT NULL,
    link_reason VARCHAR(500) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL,

    unlinked_by VARCHAR(200),
    unlink_correlation_id VARCHAR(150),
    unlink_reason VARCHAR(500),
    unlinked_at TIMESTAMPTZ,

    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_observed_master_link_observed
        FOREIGN KEY (observed_customer_id)
        REFERENCES customer_observed_customer (observed_customer_id),

    CONSTRAINT fk_observed_master_link_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_management_customer (customer_id),

    CONSTRAINT ck_observed_master_link_status
        CHECK (link_status IN ('LINKED', 'UNLINKED')),

    CONSTRAINT ck_observed_master_link_state
        CHECK (
            (
                link_status = 'LINKED'
                AND unlinked_by IS NULL
                AND unlink_correlation_id IS NULL
                AND unlink_reason IS NULL
                AND unlinked_at IS NULL
            )
            OR
            (
                link_status = 'UNLINKED'
                AND unlinked_by IS NOT NULL
                AND unlink_correlation_id IS NOT NULL
                AND unlink_reason IS NOT NULL
                AND unlinked_at IS NOT NULL
                AND unlinked_at >= linked_at
            )
        ),

    CONSTRAINT ck_observed_master_link_row_version
        CHECK (row_version >= 0)
);

CREATE INDEX ix_observed_master_link_customer
    ON customer_observed_master_link (
        customer_id,
        link_status,
        linked_at DESC
    );


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260822.04__create_customer_management_audit.sql
-- ---------------------------------------------------------------------------

CREATE TABLE customer_management_audit (
    audit_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    result VARCHAR(32) NOT NULL,
    actor_id VARCHAR(200) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_customer_management_audit_result
        CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX ix_customer_management_audit_aggregate
    ON customer_management_audit (
        aggregate_type,
        aggregate_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_actor
    ON customer_management_audit (
        actor_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_correlation
    ON customer_management_audit (
        correlation_id
    );


-- ---------------------------------------------------------------------------
-- Source folded into baseline: V20260822.05__index_customer_management_search.sql
-- ---------------------------------------------------------------------------

CREATE INDEX ix_customer_management_customer_financial_institution
    ON customer_management_customer (
                                     financial_institution_code
        );

CREATE INDEX ix_customer_management_customer_created_at
    ON customer_management_customer (
                                     created_at DESC
        );
