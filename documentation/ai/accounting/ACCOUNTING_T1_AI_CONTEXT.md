# ACCOUNTING_T1 — Current-state AI context

Status: ACTIVE CURRENT-STATE REFERENCE  
Program implementation status: IMPLEMENTED_PENDING_FINAL_VALIDATION  
Authority: supporting AI context only. Higher-priority implementation,
architecture, requirements and registered physical contracts prevail.

## Scope and ownership

- Payment owns atomic T0 execution, finalized T0 financial snapshots and final
  Payment lifecycle states.
- Accounting owns the local candidate projection, cutoff/eligibility,
  Accounting batches, provider submission/recovery and TFJ reconciliation.
- Amplitude / La Regionale is the system of record for Core Banking accounting
  submission results and TFJ end-of-day confirmation.
- Accounting never accesses Payment JPA entities, infrastructure or repositories.
- `integration` remains provider-neutral; Accounting provider DTOs/mappers stay
  in `backend/accounting`.

## Payment -> Accounting input

A T1 candidate may be created only from the approved durable semantic Payment
fact satisfying all of the following:

- Payment state `POSTED_PENDING_TFJ`;
- authoritative T0 outcome `COMPLETED`;
- finalized `PaymentFinancialEventSnapshot`;
- mandatory T0 bank posting reference;
- authoritative Core Banking accounting/business date.

The frozen snapshot is copied into Accounting. Accounting never rebuilds T1
entries from mutable Payment state.

Technical replay identity is `eventId`. Business identity is
`(paymentId, financialSnapshotId)`.

## T1 eligibility and cutoff

Candidate selection is Accounting-owned and uses frozen facts.

The TRESOR PAY status-query capability is registered as `REFERENCE_MVP` and
remains subject to its registry approval/generation policy. Its provider adapter
must not be generated while `approvalStatus`, `generationPolicy` or
`codeGenerationAllowed` prohibit generation.

Only authoritative TRESOR PAY `COMPLETED` evidence is acceptable when that
coherence check is available. Provider unavailability or a non-final status
does not invalidate completed T0; the candidate is excluded from the current
selection and may be reconsidered later.

Cutoff timezone/time are runtime configuration. No new scheduler cadence is
invented by this context.

## Accounting batch

A constituted Accounting batch freezes the candidate snapshot.

Each new batch item contains:

- `paymentId`;
- public Payment reference;
- finalized `financialSnapshotId`;
- T0 bank posting reference;
- debtor and creditor account references;
- exactly two immutable DEBIT/CREDIT accounting entries.

Stable batch idempotency is rooted in sorted
`paymentId:financialSnapshotId` identities.

Candidate `batchId` assignment occurs only after durable batch persistence.

## Core Banking accounting submission

Approved physical contract:

`documentation/contracts/amplitude/amplitude-accounting-entries-api-v1.yaml`

Submission:

`POST /api/v1/accounting-entries`

Authoritative recovery:

- `GET /api/v1/accounting-entries/batches/{batchId}`;
- `GET /api/v1/accounting-entries/idempotency/{idempotencyKey}`.

Security remains OAuth2 Client Credentials plus mTLS and the approved
correlation/institution/idempotency headers.

Provider account mapping uses canonical `age-ncp-clc`.
Canonical `DEBIT` maps to provider `D`; `CREDIT` maps to `C`.

Provider batch states:

- `ACCEPTED`;
- `PROCESSING`;
- `COMPLETED`.

Provider item states:

- `SUCCESS`;
- `FAILED`;
- `UNKNOWN`.

A `COMPLETED` provider batch does not imply that every item succeeded.
Unknown submission outcomes require authoritative lookup before any retry.
Blind financial replay is forbidden.

## TFJ ingestion and Payment finality

Approved physical contract:

`documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml`

Primary path:

`POST /webhooks/v1/amplitude/end-of-day-confirmations`

Controlled fallback:

`GET /api/v1/end-of-day-confirmations`

Amplitude is the sole TFJ system of record.

Callback evidence is authenticated by the approved transport/security profile
and is durably persisted before any Payment finality publication.

Matching uses exactly:

- `financialInstitutionCode`;
- `businessDate`;
- `paymentReference`;
- `bankPostingReference`.

Rules:

- zero matches -> `UNMATCHED`, quarantined, no Payment update;
- more than one match -> `AMBIGUOUS`, quarantined, no Payment update;
- one match -> `MATCHED`;
- identical replay -> no-op;
- same identity with different logical payload -> conflict, no Payment update;
- `PENDING` -> persisted, never final;
- uniquely matched `INTEGRATED` -> finality event after commit, Payment may reach
  `TREASURY_INTEGRATED` through its existing reconciliation use case;
- uniquely matched `FAILED` -> authoritative recovery evidence forwarded to
  Payment; Payment policy remains owner of any `REVERSAL_REQUIRED` transition.

Accounting does not directly mutate Payment.

## Finality publication recovery

A matched terminal TFJ confirmation is not marked published until the
provider-neutral integration event has been emitted successfully.

Rows with `finalityPublishedAt == null` remain recoverable through
`TfjFinalityPublicationService.publishPending`.

No public/internal HTTP recovery endpoint and no new scheduler cadence are
invented by T1.

## Observability baseline

Low-cardinality metrics:

- `sixpay.accounting.tfj.ingestion{tfj_status,receipt_status}`;
- `sixpay.accounting.tfj.conflicts`;
- `sixpay.accounting.tfj.finality.publication{outcome}`;
- `sixpay.accounting.tfj.finality.pending`.

High-cardinality business identifiers are forbidden as metric tags.

Structured logs may carry the minimum operational identifiers needed for TFJ
reconciliation. Secrets, tokens, OTP values and raw banking payloads are
forbidden.

Operational procedure:

`documentation/runbooks/accounting/ACCOUNTING_TFJ_RECONCILIATION.md`

## Validation and closure

Targeted static gate:

`py scripts/verify_accounting_t1_closure.py`

Required broader evidence:

```bash
cd backend
mvn -pl accounting,payment -am -DskipITs verify
cd ..
py scripts/verify_accounting_t1_closure.py
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_master_engineering_prompt.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

When the clean-room environment is available:

`py scripts/verify_clean_room.py`

ACCOUNTING_T1 may be reported `REPOSITORY_VALIDATED / CLOSED` only after the
required commands actually finish successfully.

## Explicit remaining external/operational constraints

- TRESOR PAY T1 status-query generation remains governed by its registry entry;
  this context does not upgrade its approval or generation policy.
- Runtime OAuth2, mTLS, certificates, provider URLs and secrets remain external
  configuration.
- Scheduler cadence for periodic status/lookup/recovery work is not invented.
- Previously persisted unmatched/ambiguous TFJ evidence has no automatic
  rematching workflow in the current baseline; manual investigation must not
  mutate Payment or use blind SQL/replay.
- Provider submission recovery must continue to respect durable local
  Accounting evidence and the no-blind-replay rule.

## Forbidden

- direct Accounting access to Payment JPA/repositories/infrastructure;
- reconstruction of T1 entries from mutable Payment state;
- full Amplitude legacy `bkmvti` schema reproduction;
- blind replay after unknown banking outcome;
- manual Payment finality updates from Accounting;
- generation from a deferred, unapproved or generation-forbidden contract;
- reintroduction of split-leg T0 posting or `DEBIT_CONFIRMED`.
