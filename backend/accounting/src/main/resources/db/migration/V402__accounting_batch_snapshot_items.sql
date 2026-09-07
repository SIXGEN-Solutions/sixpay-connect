ALTER TABLE accounting_batch_items
    ADD COLUMN financial_snapshot_id UUID NULL,
    ADD COLUMN financial_snapshot_version VARCHAR(32) NULL,
    ADD COLUMN financial_snapshot_finalized_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN debtor_account_reference VARCHAR(256) NULL,
    ADD COLUMN creditor_account_reference VARCHAR(256) NULL;

CREATE INDEX idx_accounting_batch_items_financial_snapshot_id
    ON accounting_batch_items(financial_snapshot_id);

CREATE TABLE accounting_batch_item_entries (
    id UUID PRIMARY KEY,
    batch_item_id UUID NOT NULL,
    entry_snapshot_id UUID NOT NULL,
    entry_sequence INTEGER NOT NULL,
    direction VARCHAR(16) NOT NULL,
    account_reference VARCHAR(256) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_accounting_batch_item_entries_item
        FOREIGN KEY (batch_item_id) REFERENCES accounting_batch_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_accounting_batch_item_entries_snapshot
        UNIQUE (batch_item_id, entry_snapshot_id),
    CONSTRAINT uk_accounting_batch_item_entries_sequence
        UNIQUE (batch_item_id, entry_sequence),
    CONSTRAINT ck_accounting_batch_item_entries_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_accounting_batch_item_entries_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_accounting_batch_item_entries_batch_item
    ON accounting_batch_item_entries(batch_item_id);
