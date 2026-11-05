-- SIXPAY Payment: selected T0 banking fields for immutable BKEVE/BKMVTI snapshots.
-- Additive, nullable for historical snapshots. ISO currency remains in requested_currency/currency.
-- No fees, commissions, taxes, duplicate amounts or duplicate directional codes.
ALTER TABLE payment_financial_event_snapshots
    ADD COLUMN operation_code VARCHAR(3),
    ADD COLUMN nature VARCHAR(32),
    ADD COLUMN technical_user VARCHAR(64),
    ADD COLUMN debit_amount NUMERIC(38,18),
    ADD COLUMN credit_amount NUMERIC(38,18),
    ADD COLUMN event_label VARCHAR(256),
    ADD COLUMN bank_event_number VARCHAR(32),
    ADD COLUMN accounting_date DATE,
    ADD COLUMN operation_date DATE,
    ADD COLUMN origin_branch VARCHAR(20);

ALTER TABLE payment_financial_entry_snapshots
    ADD COLUMN bkmvti_age VARCHAR(20),
    ADD COLUMN bkmvti_cha VARCHAR(20),
    ADD COLUMN bkmvti_ncp VARCHAR(30),
    ADD COLUMN bkmvti_suf VARCHAR(10),
    ADD COLUMN bkmvti_clc VARCHAR(10),
    ADD COLUMN bkmvti_ope VARCHAR(3),
    ADD COLUMN bkmvti_mvti VARCHAR(20),
    ADD COLUMN bkmvti_uti VARCHAR(64),
    ADD COLUMN bkmvti_eve VARCHAR(32),
    ADD COLUMN bkmvti_dco DATE,
    ADD COLUMN bkmvti_dva DATE,
    ADD COLUMN bkmvti_lib VARCHAR(256),
    ADD COLUMN bkmvti_exo VARCHAR(10),
    ADD COLUMN bkmvti_pie VARCHAR(100),
    ADD COLUMN bkmvti_agsa VARCHAR(20),
    ADD COLUMN bkmvti_agde VARCHAR(20),
    ADD COLUMN bkmvti_devc VARCHAR(3),
    ADD COLUMN bkmvti_mctv NUMERIC(38,18),
    ADD COLUMN bkmvti_nat VARCHAR(32),
    ADD COLUMN bkmvti_eta VARCHAR(10);

COMMENT ON TABLE payment_financial_event_snapshots IS
    'Payment-owned immutable T0 event facts with selected bank field extensions; not a full Amplitude BKEVE table.';
COMMENT ON TABLE payment_financial_entry_snapshots IS
    'Payment-owned immutable T0 accounting-entry facts for T1; not a full Amplitude BKMVTI table.';
