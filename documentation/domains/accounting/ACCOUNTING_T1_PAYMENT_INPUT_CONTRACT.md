# ACCOUNTING_T1 — Internal Payment → Accounting input contract

## Purpose

Define the only approved semantic facts that Accounting may consume from Payment for T1.
This is an internal modular-monolith contract, not an external HTTP/provider contract.

## Boundary

Producer: `payment`
Consumer: `accounting`
Mode: asynchronous durable event
Delivery: push across the module boundary
Accounting batch selection: pull from Accounting-owned local projection only

Direct Accounting access to Payment JPA entities, Spring Data repositories, infrastructure adapters or private domain repositories is forbidden.

## Publication preconditions

All conditions are mandatory:

- Payment status is `POSTED_PENDING_TFJ`.
- Authoritative T0 outcome is `COMPLETED`.
- T0 bank reference is non-blank.
- `PaymentFinancialEventSnapshot` exists.
- financial snapshot status is `FINALIZED`.
- authoritative Core Banking accounting date used by T0 is available.

## Contract shape v1

The semantic fact contains:

- event identity: `eventId`, `occurredAt`, schema version;
- Payment identity: `paymentId`, `publicPaymentReference`, partner identity, financial institution code;
- T0 evidence: outcome `COMPLETED`, bank reference, observed instant, authoritative accounting date;
- financial snapshot identity: `financialSnapshotId`, snapshot version, finalized instant;
- frozen financial facts: debtor account reference, creditor/Treasury account reference, requested amount/currency;
- frozen ordered entries: entry snapshot id, sequence, direction, account reference, amount/currency, creation instant.

The event contract must not expose Payment aggregate objects, Payment persistence documents, JPA entities or repositories.

## Idempotence

- Technical replay identity: `eventId`. Same event replay is a no-op for the consumer.
- Business identity: `(paymentId, financialSnapshotId)`. A different eventId carrying the same business identity must converge on the same local Accounting candidate fact.
- Persistence constraints for the projection are deferred to T1.2 and require explicit schema approval.

## Ownership

Payment owns the T0 outcome, bank reference and financial snapshots.
Accounting owns only its copied projection, eligibility decisions, batch lifecycle, submission tracking and reconciliation state.
Payment remains owner of final Payment states including `TREASURY_INTEGRATED` and `REVERSAL_REQUIRED`.

## Explicit exclusions

T1.0 does not define or authorize:

- a TRESOR PAY status endpoint;
- a Core Banking Accounting endpoint;
- provider DTOs or provider mappings;
- a bkmvti persistence entity inside SIXPAY;
- direct Payment repository access from Accounting;
- a blind retry policy for an unknown external accounting submission.

## T1.2 persistence realization

T1.2 materializes the approved Payment -> Accounting fact into the Accounting-owned
tables `accounting_payment_candidates` and `accounting_payment_candidate_entries`.

Projection creation uses the T1.0 publication preconditions. TRESOR PAY `COMPLETED`,
cutoff-window membership and absence of batch assignment are selection-time criteria,
not projection-publication preconditions.

Technical replay is protected by unique `event_id`; business convergence is protected
by unique `(payment_id, financial_snapshot_id)`. Accounting never reads Payment JPA,
repositories or infrastructure.

## T1.3 batch constitution realization

T1.3 keeps the existing cutoff, eligibility, builder, idempotency and batch persistence flow,
but freezes the T1.2 financial snapshot identity and ordered entries into each newly
constituted Accounting batch item.

Active rules:
- candidates come only from the Accounting-owned T1.2 projection;
- cutoff membership, financial institution, TRESOR PAY `COMPLETED` and no prior batch assignment are selection-time criteria;
- a new T1.3 batch item requires a finalized financial snapshot identity and its two frozen entries;
- batch idempotency binds to `(paymentId, financialSnapshotId)` identities;
- after durable batch persistence, selected candidates are assigned to that batch transactionally;
- historical pre-T1.3 batch rows are not backfilled by V402;
- no Core Banking T1 provider mapping is defined before T1.4.
