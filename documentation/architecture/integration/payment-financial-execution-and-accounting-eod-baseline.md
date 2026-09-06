# SIXPAY CONNECT — Payment Financial Execution and Accounting EOD Baseline

## Status

- Classification: `CANONICAL`
- Scope: Payment financial execution T0 and Accounting end-of-day T+1
- Decision source: PAYMENT_COMPLETION / LOT 2.3 functional alignment
- MVP accounting delivery mode: Core Banking API
- Deferred accounting delivery mode: CSV/file exchange
- Implementation status: documentation/contract alignment only; Java implementation is not changed by this decision

## 1. Fundamental separation

SIXPAY distinguishes two different capabilities.

### T0 — synchronous financial execution

T0 is one protected synchronous Core Banking financial command.

Before provider submission, Payment freezes reduced immutable financial-event
and financial-entry snapshots. These are Payment-owned historical execution
facts; they are not the Payment aggregate and they are not copies of the full
historical Amplitude physical schema.

The Payment-owned Amplitude anti-corruption layer maps those snapshots to the
provider event payload corresponding functionally to:

- `bkeve`: the Core Banking Payment event;
- `bkmvti[]`: the debit/credit accounting lines attached to that event.

Only the provider attributes required by the approved SIXPAY mapping are stored.
Provider-specific values that influence the emitted payload and may change over
time must be frozen in the snapshot before submission.

The Core Banking system remains authoritative for all execution-time banking
controls and for the atomic execution of the submitted financial event.

Mandatory controls:

1. `ACCOUNT_EXISTS`
2. `ACCOUNT_ACTIVE`
3. `DEBIT_ALLOWED`
4. `CURRENCY_SUPPORTED`
5. `AVAILABLE_FUNDS_SUFFICIENT`
6. `PER_TRANSACTION_LIMIT_NOT_EXCEEDED`
7. `DAILY_LIMIT_NOT_EXCEEDED`
8. `OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED`

There is no standalone read-only Funds Control call in the MVP between OTP
confirmation and debit/credit.

If any mandatory control fails, Core Banking returns a conclusive business
rejection and no successful financial execution is recorded.

If every mandatory control passes, Core Banking validates the submitted debtor
and protected Partner creditor references and atomically executes the submitted
debit/credit event.

A timeout or transport failure after submission does not prove failure.
Unknown financial outcomes are recovered by authoritative lookup and are never
blindly resubmitted.

The approved T0 physical resource is:

- `POST /api/v1/payment-events`

Both authoritative recovery mechanisms are retained:

- `GET /api/v1/payment-events/{paymentReference}`;
- `GET /api/v1/payment-events/idempotency/{idempotencyKey}`.

T0 application integration security is OAuth2 Client Credentials plus mTLS.
Bank-SI network controls remain defense in depth and do not replace application
authentication and authorization.

### T+1 — Accounting end-of-day treatment

T+1 is independent from T0 financial execution.

Only payments whose T0 financial execution succeeded are candidates.

The immutable financial-entry snapshots created for T0 are the source of the
accounting lines later selected for T+1. Accounting must not reconstruct those
lines from mutable current Payment, Partner or provider-configuration state.

Accounting:

1. selects eligible successful Payments for the applicable cut-off/business date;
2. obtains or uses authoritative TRESOR PAY status evidence for each candidate;
3. retains only candidates whose TRESOR PAY status is compatible with accounting eligibility;
4. obtains the immutable T0 financial-entry snapshot facts through an approved Payment-to-Accounting boundary;
5. builds an Accounting batch containing the frozen accounting lines;
6. submits that batch to the Core Banking accounting capability;
7. tracks provider acknowledgement, rejection and unknown outcomes;
8. reconciles the final accounting result.

SIXPAY owns generation and durable freezing of the accounting-line instructions
associated with its Payment event. Core Banking remains authoritative for
validation, acceptance, effective accounting posting and final accounting result.

A T+1 accounting failure does not retroactively turn a successful T0 financial
execution into an unpaid Payment.

## 2. MVP delivery mode and deferred file mode

### MVP

The MVP uses the existing Accounting API integration shape:

`AccountingBatchGateway -> RestAccountingBatchClient -> Core Banking accounting API`

The physical provider endpoint/path and final wire contract remain contract
items to finalize. No endpoint or provider field is invented by this document.

### Deferred evolution

CSV/file submission is a supported future delivery option only after the Core
Banking team defines:

- accounting line layout;
- field semantics;
- encoding;
- filename convention;
- totals/control records;
- integrity/signature/checksum;
- SFTP or equivalent transport;
- acknowledgement/rejection format;
- idempotency and reconciliation rules.

CSV is not the MVP transport.

## 3. Interpretation of the existing Payment lifecycle

The existing lifecycle names are preserved until an implementation lot explicitly
changes them.

For alignment purposes:

- `FUNDS_CONTROL_PENDING` remains a Payment business stage expressing that the
  mandatory execution-time financial controls are required. It does not imply a
  standalone Core Banking read-only call.
- `TREASURY_ACCOUNT_RESOLUTION_PENDING` remains the Payment stage in which the
  protected Treasury destination is resolved/prepared. No prior positive balance
  check may be relied on as a reservation.
- `APPROVED_FOR_POSTING` means SIXPAY has all internal prerequisites required to
  submit T0.
- `POSTING_PENDING` is the existing lifecycle name for the T0 Core Banking
  financial command. In this baseline it means checks + protected Treasury
  resolution/use + customer debit + Treasury credit.
- `POSTING_OUTCOME_UNKNOWN` remains the unknown T0 financial outcome state.
- `DEBIT_CONFIRMED` continues to represent confirmed financial effect when the
  overall financial outcome is not yet fully conclusive.
- `POSTED_PENDING_TFJ` means T0 financial execution is known and the payment is
  awaiting the end-of-day Accounting/TFJ lifecycle.
- `TREASURY_INTEGRATED` is reserved for a successfully matched authoritative
  end-of-day accounting/TFJ result.

The existing names are not renamed by this documentation-only alignment.

## 4. Ownership

- Payment owns the T0 business lifecycle, immutable reduced financial-event/entry snapshots and provider-specific event mapping.
- Amplitude/Core Banking is authoritative for account state, funds, limits, execution acceptance, effective debit/credit and banking references.
- Accounting owns candidate selection, TRESOR PAY status evidence used for accounting eligibility, consumption of immutable Payment financial-entry facts, batch constitution, submission tracking and reconciliation.
- Core Banking owns validation and effective accounting posting of the T1 lines submitted by SIXPAY.
- TRESOR PAY does not need to know Core Banking accounting internals. It receives
  only the appropriate Payment/Treasury integration status and failure reason
  through approved SIXPAY-facing contracts.

## 5. Contract governance

This decision changes the semantics previously associated with the physical
`amplitude-payment-posting-api-v1` and end-of-day documentation.

### T0 Payment Event

The T0 Core Banking Payment Event contract is approved for implementation:

- `amplitude-payment-posting-api-v1` remains the stable registry/file identity;
- `lifecycleStatus` is `ACTIVE_MVP`;
- `approvalStatus` is `APPROVED`;
- `generationPolicy` is `ACTIVE`;
- `codeGenerationAllowed` is `true`;
- the approved physical operations are `POST /api/v1/payment-events`,
  `GET /api/v1/payment-events/{paymentReference}` and
  `GET /api/v1/payment-events/idempotency/{idempotencyKey}`.

Implementation may therefore proceed against the approved T0 logical contract.

The exact reduced provider field subset/code tables used to map SIXPAY
financial-event and financial-entry snapshots to the `bkeve`/`bkmvti`-equivalent
payload remain implementation mapping inputs. Their absence does not revert the
approved T0 lifecycle/generation status, but no provider field or code may be
invented.

### T+1 Accounting

The physical Core Banking Accounting batch-submission contract remains
`TO_DEFINE`.

Therefore:

- no T+1 provider endpoint or wire schema is authorized by this T0 approval;
- no T+1 provider client/server code may be generated from a nonexistent
  physical contract;
- `amplitude-end-of-day-confirmation-api-v1` remains a separate
  result/reconciliation contract subject to its own approval state.

## 6. Non-goals

This baseline does not:

- modify Java implementation;
- add a database migration;
- rename Payment statuses;
- define a new public TRESOR PAY endpoint;
- invent a Core Banking endpoint or field;
- enable CSV transport;
- authorize blind retry of financial commands;
- change OTP ownership or confirmation semantics.
