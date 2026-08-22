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
