-- T0.3: independent durable callback progress on the existing Payment outbox.
-- Existing status, claims and ordering remain owned by the generic relay.
-- Callback delivery is relevant only to payment.domain-event records.
ALTER TABLE payment_outbox_events
    ADD COLUMN callback_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN callback_attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN callback_next_attempt_at TIMESTAMPTZ,
    ADD COLUMN callback_last_attempt_at TIMESTAMPTZ,
    ADD COLUMN callback_claimed_at TIMESTAMPTZ,
    ADD COLUMN callback_claimed_by VARCHAR(100),
    ADD COLUMN callback_published_at TIMESTAMPTZ,
    ADD COLUMN callback_failure_reason VARCHAR(1000);

UPDATE payment_outbox_events
   SET callback_next_attempt_at = created_at;
ALTER TABLE payment_outbox_events ALTER COLUMN callback_next_attempt_at SET NOT NULL;

ALTER TABLE payment_outbox_events
    ADD CONSTRAINT ck_payment_outbox_callback_status
        CHECK (callback_status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD')),
    ADD CONSTRAINT ck_payment_outbox_callback_attempts
        CHECK (callback_attempt_count >= 0),
    ADD CONSTRAINT ck_payment_outbox_callback_claim
        CHECK ((callback_status = 'PROCESSING' AND callback_claimed_at IS NOT NULL
                 AND callback_claimed_by IS NOT NULL)
               OR (callback_status <> 'PROCESSING' AND callback_claimed_at IS NULL
                   AND callback_claimed_by IS NULL)),
    ADD CONSTRAINT ck_payment_outbox_callback_published
        CHECK ((callback_status = 'PUBLISHED' AND callback_published_at IS NOT NULL)
               OR (callback_status <> 'PUBLISHED' AND callback_published_at IS NULL));

CREATE INDEX idx_payment_outbox_callback_claimable
    ON payment_outbox_events (callback_status, callback_next_attempt_at, occurred_at)
    WHERE event_type = 'payment.domain-event';

COMMENT ON COLUMN payment_outbox_events.callback_status IS
    'Callback delivery progress, independent of the generic outbox status.';
