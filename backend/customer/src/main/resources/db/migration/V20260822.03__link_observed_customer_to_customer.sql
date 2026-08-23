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
