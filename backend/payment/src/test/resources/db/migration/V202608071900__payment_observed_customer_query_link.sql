CREATE TABLE payment_observed_customer_link (
    payment_id UUID PRIMARY KEY,
    observed_customer_id UUID NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_observed_customer_link_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_payment_observed_customer_link_customer
    ON payment_observed_customer_link(observed_customer_id, payment_id);
