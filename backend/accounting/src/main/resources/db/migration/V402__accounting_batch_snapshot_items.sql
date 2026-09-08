ALTER TABLE accounting_batch_items
    ADD COLUMN IF NOT EXISTS financial_snapshot_id UUID,
    ADD COLUMN IF NOT EXISTS financial_snapshot_version VARCHAR(32),
    ADD COLUMN IF NOT EXISTS financial_snapshot_finalized_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS debtor_account_reference VARCHAR(256),
    ADD COLUMN IF NOT EXISTS creditor_account_reference VARCHAR(256);

CREATE TABLE IF NOT EXISTS accounting_batch_item_entries (
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
    CONSTRAINT uk_accounting_batch_item_entries_snapshot UNIQUE (batch_item_id, entry_snapshot_id),
    CONSTRAINT uk_accounting_batch_item_entries_sequence UNIQUE (batch_item_id, entry_sequence),
    CONSTRAINT ck_accounting_batch_item_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_accounting_batch_item_entries_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_item_entries_item
    ON accounting_batch_item_entries(batch_item_id);

CREATE INDEX IF NOT EXISTS idx_accounting_batch_items_financial_snapshot
    ON accounting_batch_items(financial_snapshot_id);
