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
