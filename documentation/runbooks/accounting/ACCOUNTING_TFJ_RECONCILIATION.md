# Accounting TFJ Reconciliation Runbook

## Purpose

This runbook covers operational investigation of Accounting T1 / TFJ
reconciliation in SIXPAY CONNECT.

Amplitude / La Régionale remains the authoritative system of record for the
end-of-day treasury confirmation. SIXPAY persists the confirmation evidence,
matches it against durable Accounting evidence and publishes Payment finality
only when the approved matching and finality rules are satisfied.

This procedure does not authorize a financial replay, a manual Payment state
change, or a direct database correction.

## Authoritative flow

```text
SIXPAY Accounting batch submission
  -> Core Banking processing
  -> Amplitude TFJ confirmation
  -> SIXPAY TFJ ingestion
  -> durable matching result
  -> finality publication
  -> Payment reconciliation
```

The authoritative TFJ contract is:

`documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml`

The Accounting submission/recovery contract is:

`documentation/contracts/amplitude/amplitude-accounting-entries-api-v1.yaml`

## Matching keys

A TFJ confirmation is matched using exactly:

- `financialInstitutionCode`;
- `businessDate`;
- `paymentReference`;
- `bankPostingReference`.

Do not attempt to match on mutable Payment state or on alternative identifiers.

## Operational states

### MATCHED

Exactly one durable Accounting candidate matches the confirmation.

- `PENDING` remains non-final.
- `INTEGRATED` may publish Payment finality after the confirmation has been
  durably persisted.
- `FAILED` remains authoritative recovery evidence and is forwarded to Payment
  through the existing finality mechanism. Accounting does not directly mutate
  Payment.

### UNMATCHED

No durable Accounting record matches the four canonical keys.

Operational action:

1. preserve the TFJ confirmation as quarantined evidence;
2. verify the institution, business date, Payment reference and bank posting
   reference against the durable Accounting batch;
3. verify whether the corresponding T1 batch was submitted or is still
   recoverable through the approved Core Banking lookup operations;
4. do not create a synthetic Accounting row;
5. do not update Payment manually;
6. do not replay a financial submission blindly.

An `UNMATCHED` confirmation never changes Payment.

### AMBIGUOUS

More than one durable Accounting record matches the canonical identity.

Operational action:

1. preserve the confirmation as quarantined evidence;
2. inspect the duplicate durable Accounting facts;
3. verify batch and projection identity before any remediation;
4. escalate the data-integrity issue if more than one durable candidate remains;
5. do not select one candidate manually;
6. do not mutate Payment.

An `AMBIGUOUS` confirmation never changes Payment.

## Replay handling

### Identical replay

An identical confirmation received again for the same logical identity is a
no-op.

The already persisted evidence remains authoritative and no duplicate Payment
finality event must be emitted.

### Conflicting replay

A confirmation with the same logical identity but a different logical payload
is a conflict.

Operational action:

1. retain the conflicting evidence for investigation;
2. do not replace the previously persisted authoritative confirmation;
3. do not update Payment;
4. do not replay the Accounting submission;
5. escalate for reconciliation with La Régionale / Amplitude.

## Finality publication recovery

A matched terminal TFJ confirmation is not considered published until its
Payment finality event has been emitted successfully.

The durable marker is:

`finalityPublishedAt`

Interpretation:

- `finalityPublishedAt != null`: the finality publication completed;
- `finalityPublishedAt == null`: the terminal confirmation remains pending
  publication and is recoverable through the existing
  `TfjFinalityPublicationService.publishPending` mechanism.

No public or internal HTTP recovery endpoint exists for this recovery path.

Do not introduce an ad-hoc HTTP endpoint, direct SQL update, or manual Payment
transition as an operational shortcut.

## Provider submission recovery

When the outcome of a Core Banking Accounting submission is unknown:

1. use the authoritative provider lookup by idempotency key first when
   applicable;
2. use the approved batch lookup as defined by the provider contract;
3. reconcile the durable local batch with the provider result;
4. never repeat the financial POST blindly.

The local Accounting batch idempotency key remains the canonical financial
idempotency identity.

## Manual T1 execution

The operator command **Lancer le traitement T1** launches the SIXPAY T1
treatment for one `businessDate`.

It does not launch the bank-owned TFJ.

The command:

- requires `ADMIN` or `MANAGER`;
- requires `accounting.t1.execute`;
- runs the existing `MANUAL` cutoff path;
- derives the financial institution from durable Accounting candidates;
- reuses existing batch idempotency;
- submits or reconciles through the approved Core Banking Accounting boundary.

`AUDITOR` remains read-only.

## Observability

Use the existing low-cardinality metrics:

- `sixpay.accounting.tfj.ingestion{tfj_status,receipt_status}`;
- `sixpay.accounting.tfj.conflicts`;
- `sixpay.accounting.tfj.finality.publication{outcome}`;
- `sixpay.accounting.tfj.finality.pending`.

Do not add Payment identifiers, confirmation identifiers or other
high-cardinality business identifiers as metric tags.

Structured logs may contain only the minimum operational identifiers required
for reconciliation. Do not log secrets, tokens, OTP values or raw banking
payloads.

## Forbidden operational actions

The following actions are not authorized by this runbook:

- direct Accounting access to Payment JPA entities or repositories;
- manual Payment finality mutation from Accounting;
- blind financial replay after an unknown provider outcome;
- fabrication or alteration of TFJ evidence;
- direct database changes to force `MATCHED`, `INTEGRATED` or publication;
- creation of a public or internal HTTP recovery endpoint outside an approved
  contract and implementation lot.
