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

The command is atomic from the SIXPAY business perspective. The Core Banking
system evaluates all mandatory execution controls and, only when every control
passes, resolves the protected Treasury/CUT destination and executes the
customer debit and Treasury credit.

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

This is required because other banking channels and applications can change the
same customer account concurrently. A prior balance/funds check cannot reserve
financial truth for a later debit.

If any mandatory control fails:

- no debit is authorized;
- no Treasury credit is authorized;
- Core Banking returns a business rejection and authoritative reason.

If every mandatory control passes, the same Core Banking command:

- resolves/uses the protected configured Treasury destination;
- debits the customer account;
- credits the Treasury account;
- returns the authoritative financial outcome and bank reference.

A timeout or transport failure after submission does not prove failure.
Unknown financial outcomes are recovered by authoritative lookup and are never
blindly resubmitted.

### T+1 — Accounting end-of-day treatment

T+1 is independent from T0 financial execution.

Only payments whose T0 financial execution succeeded are candidates.

Accounting:

1. selects eligible successful Payments for the applicable cut-off/business date;
2. obtains or uses authoritative TRESOR PAY status evidence for each candidate;
3. retains only candidates whose TRESOR PAY status is compatible with accounting eligibility;
4. builds an Accounting batch;
5. submits the payment batch to the Core Banking accounting capability;
6. tracks provider acknowledgement, rejection and unknown outcomes;
7. reconciles the final accounting result.

SIXPAY does not generate Core Banking debit/credit accounting entries for the MVP.
The Core Banking accounting API receives eligible payment transactions and owns
the generation and posting of accounting entries into Core Banking tables.

A T+1 accounting failure does not retroactively turn a successful T0 financial
execution into an unpaid Payment. It is an accounting/reconciliation outcome
requiring its own status, reason and recovery path.

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

- Payment owns the T0 business lifecycle and its Core Banking financial-execution orchestration.
- Amplitude/Core Banking is authoritative for account state, funds, limits,
  financial entries and bank references.
- Accounting owns candidate selection, TRESOR PAY status evidence used for
  accounting eligibility, batch constitution, submission tracking and reconciliation.
- Core Banking owns accounting-entry generation and posting.
- TRESOR PAY does not need to know Core Banking accounting internals. It receives
  only the appropriate Payment/Treasury integration status and failure reason
  through approved SIXPAY-facing contracts.

## 5. Contract governance

This decision changes the semantics previously associated with the physical
`amplitude-payment-posting-api-v1` and end-of-day documentation.

Until the revised wire contracts are reviewed:

- affected external Core Banking contracts remain `REFERENCE_ONLY`;
- `codeGenerationAllowed` remains `false`;
- no Core Banking client/server code may be generated from the revised target
  semantics solely because this document exists;
- unresolved provider endpoint, payload, code table and operational parameters
  remain explicit contract work.

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
