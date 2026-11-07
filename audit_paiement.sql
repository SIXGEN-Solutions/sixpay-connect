\set payment_ref 'PAY-94EAD358B508409D96197522DC'

-- Contrôle 1 — Vérifier l'état courant du Payment
SELECT
    payment_id,
    public_payment_reference,
    payment_source,
    external_payment_reference,
    partner_identifier,
    external_subscription_reference,
    financial_institution_code,
    requested_amount,
    requested_currency,
    status,
    business_version,
    persistence_version,
    received_at,
    updated_at,
    finalized_at
FROM payments
WHERE public_payment_reference = :'payment_ref';

-- Contrôle 2 — Reconstituer tous les événements métier
SELECT
    a.business_version,
    a.event_sequence,
    a.event_type,
    a.payment_status,
    a.event_id,
    a.correlation_id,
    a.causation_id,
    a.occurred_at
FROM payment_audit a
JOIN payments p
    ON p.payment_id = a.payment_id
WHERE p.public_payment_reference = :'payment_ref'
ORDER BY
    a.business_version,
    a.event_sequence;

-- Contrôle 3 — Vérifier la preuve bancaire COMPLETED
SELECT
    a.event_id,
    a.event_type,
    a.payment_status,
    a.business_version,
    a.occurred_at,
    o.payload ->> 'outcome' AS banking_outcome,
    o.payload ->> 'principalBankReference' AS bank_reference,
    o.payload ->> 'accountingDate' AS accounting_date,
    o.payload ->> 'observedAt' AS observed_at,
    o.payload ->> 'observationSource' AS observation_source,
    o.payload ->> 'reasonCode' AS reason_code
FROM payment_audit a
JOIN payments p
    ON p.payment_id = a.payment_id
JOIN payment_outbox_events o
    ON o.event_id = a.event_id
WHERE p.public_payment_reference = :'payment_ref'
  AND a.event_type = 'PaymentEventOutcomeRecorded'
ORDER BY a.business_version DESC;

-- Contrôle 4 — Vérifier l'événement financier T0
SELECT
    s.snapshot_id,
    s.payment_id,
    s.public_payment_reference,
    s.financial_institution_code,
    s.snapshot_version,
    s.snapshot_status,
    s.operation_code,
    s.nature,
    s.technical_user,
    s.debtor_account_reference,
    s.creditor_account_reference,
    s.requested_amount,
    s.requested_currency,
    s.debit_amount,
    s.credit_amount,
    s.bank_event_number,
    s.accounting_date,
    s.operation_date,
    s.origin_branch,
    s.created_at,
    s.finalized_at
FROM payment_financial_event_snapshots s
WHERE s.public_payment_reference = :'payment_ref';

-- Contrôle 5 — Vérifier les deux écritures comptables
SELECT
    s.public_payment_reference,
    e.entry_snapshot_id,
    e.entry_sequence,
    e.direction,
    e.account_reference,
    e.amount,
    e.currency,
    e.bkmvti_age,
    e.bkmvti_ncp,
    e.bkmvti_clc,
    e.bkmvti_ope,
    e.bkmvti_eve,
    e.bkmvti_dco,
    e.bkmvti_dva,
    e.bkmvti_nat,
    e.bkmvti_eta,
    e.created_at
FROM payment_financial_entry_snapshots e
JOIN payment_financial_event_snapshots s
    ON s.snapshot_id = e.event_snapshot_id
WHERE s.public_payment_reference = :'payment_ref'
ORDER BY e.entry_sequence;

-- Contrôle automatique de cohérence
SELECT
    s.public_payment_reference,
    COUNT(e.entry_snapshot_id) AS entry_count,

    COUNT(*) FILTER (
        WHERE e.direction = 'DEBIT'
    ) AS debit_entries,

    COUNT(*) FILTER (
        WHERE e.direction = 'CREDIT'
    ) AS credit_entries,

    SUM(e.amount) FILTER (
        WHERE e.direction = 'DEBIT'
    ) AS total_debit,

    SUM(e.amount) FILTER (
        WHERE e.direction = 'CREDIT'
    ) AS total_credit,

    s.requested_amount,
    s.accounting_date,

    COUNT(*) FILTER (
        WHERE e.bkmvti_dco = s.accounting_date
    ) AS entries_with_matching_dco

FROM payment_financial_event_snapshots s
LEFT JOIN payment_financial_entry_snapshots e
    ON e.event_snapshot_id = s.snapshot_id
WHERE s.public_payment_reference = :'payment_ref'
GROUP BY
    s.public_payment_reference,
    s.requested_amount,
    s.accounting_date;

-- Contrôle 6 — Vérifier la publication et le callback
SELECT
    o.event_id,
    a.event_type AS business_event_type,
    a.payment_status,
    o.event_type AS outbox_event_type,
    o.status AS generic_outbox_status,
    o.callback_status,
    o.callback_attempt_count,
    o.callback_last_attempt_at,
    o.callback_published_at,
    o.callback_next_attempt_at,
    o.callback_failure_reason,
    o.correlation_id,
    o.occurred_at
FROM payment_outbox_events o
JOIN payments p
    ON p.payment_id = o.aggregate_id
LEFT JOIN payment_audit a
    ON a.event_id = o.event_id
WHERE p.public_payment_reference = :'payment_ref'
  AND o.event_id =
      '5bf655e8-8aa5-4506-8a14-7c7369a31fc8'::uuid;
	  
-- Contrôler toute l'outbox du paiement
SELECT
    a.business_version,
    a.event_sequence,
    a.event_type,
    a.payment_status,
    o.event_id,
    o.status AS generic_status,
    o.callback_status,
    o.callback_attempt_count,
    o.callback_failure_reason,
    o.occurred_at
FROM payment_outbox_events o
JOIN payments p
    ON p.payment_id = o.aggregate_id
LEFT JOIN payment_audit a
    ON a.event_id = o.event_id
WHERE p.public_payment_reference = :'payment_ref'
ORDER BY o.occurred_at, o.event_id;

-- Contrôle 7 — Vérifier le contenu métier du callback enregistré
-- Pour inspecter le contenu exact reçu et enregistré dans SQLite, utilise ce script depuis le dossier callback-sandbox

@'
import sqlite3
import json

event_id = "5bf655e8-8aa5-4506-8a14-7c7369a31fc8"

db = sqlite3.connect("callback.sqlite3")
db.row_factory = sqlite3.Row

try:
    event = db.execute(
        "SELECT * FROM events WHERE event_id = ?",
        (event_id,)
    ).fetchone()

    if event is None:
        print("Événement introuvable dans SQLite")
    else:
        result = dict(event)
        for key, value in result.items():
            if isinstance(value, bytes):
                if key == "raw_body":
                    try:
                        result[key] = json.loads(value)
                    except (ValueError, UnicodeDecodeError):
                        result[key] = "<binary>"
                else:
                    result[key] = "<binary>"

        print(json.dumps(
            result,
            indent=2,
            ensure_ascii=False,
            default=str
        ))
finally:
    db.close()
'@ | py -

-- Contrôle 8 — Vérifier la cohérence complète avec une seule requête
WITH p AS (
    SELECT *
    FROM payments
    WHERE public_payment_reference = :'payment_ref'
),
posting AS (
    SELECT
        a.payment_id,
        a.event_id,
        a.payment_status,
        o.payload ->> 'outcome' AS banking_outcome,
        o.payload ->> 'principalBankReference' AS bank_reference,
        o.payload ->> 'accountingDate' AS banking_dco,
        o.callback_status,
        o.callback_attempt_count,
        o.callback_published_at
    FROM payment_audit a
    JOIN payment_outbox_events o
        ON o.event_id = a.event_id
    WHERE a.event_type = 'PaymentEventOutcomeRecorded'
      AND a.payment_status = 'POSTED_PENDING_TFJ'
),
entries AS (
    SELECT
        s.payment_id,
        COUNT(e.entry_snapshot_id) AS entry_count,
        COUNT(*) FILTER (
            WHERE e.bkmvti_dco = s.accounting_date
        ) AS matched_dco_count,
        SUM(e.amount) FILTER (
            WHERE e.direction = 'DEBIT'
        ) AS total_debit,
        SUM(e.amount) FILTER (
            WHERE e.direction = 'CREDIT'
        ) AS total_credit
    FROM payment_financial_event_snapshots s
    LEFT JOIN payment_financial_entry_snapshots e
        ON e.event_snapshot_id = s.snapshot_id
    GROUP BY s.payment_id
)
SELECT
    p.public_payment_reference,
    p.status AS current_payment_status,
    posting.banking_outcome,
    posting.bank_reference,
    posting.banking_dco,
    posting.callback_status,
    posting.callback_attempt_count,
    posting.callback_published_at,
    entries.entry_count,
    entries.matched_dco_count,
    entries.total_debit,
    entries.total_credit
FROM p
LEFT JOIN posting
    ON posting.payment_id = p.payment_id
LEFT JOIN entries
    ON entries.payment_id = p.payment_id
ORDER BY posting.callback_published_at DESC NULLS LAST;


SIXPAY - PAY-94EAD358B508409D96197522DC
[OK] Payment identifié avec le bon partenaire, la bonne souscription et le bon montant
[OK] Chronologie métier complète dans payment_audit
[OK] Événement PaymentEventOutcomeRecorded avec outcome COMPLETED
[OK] Référence bancaire et date comptable DCO présentes
[OK] Snapshot financier FINALIZED et enrichi avec les champs bancaires
[OK] Deux écritures DEBIT / CREDIT, montants équilibrés
[OK] DCO des deux écritures identique à celle de l'événement
[OK] Callback CUT_CREDITED en statut PUBLISHED, sans échec
[OK] Même eventId dans PostgreSQL et SQLite, HTTP 204 confirmé
[OK] Aucune anomalie résiduelle inattendue dans l'outbox du paiement