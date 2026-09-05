# CB-1 — Core Banking API Baseline

## Purpose

This document is the authoritative architecture baseline for Core Banking APIs
consumed by SIXPAY CONNECT under the task-selected authoritative revision.

## Governance decision D00-CB-OWNERSHIP

The Core Banking APIs required by SIXPAY are target integration contracts
designed within the SIXPAY/Core Banking programme and implemented by the
Core Banking delivery team.

They are not treated as pre-existing provider APIs whose wire contracts must
first be discovered from La Regionale.

Consequences:

- SIXPAY may define the target endpoint, method, schemas, status/error model,
  idempotency, recovery and security contract required by its use cases;
- SIXPAY may implement the corresponding outbound clients once a target
  contract is approved and code generation is explicitly allowed;
- the Core Banking team implements the server side against the approved target
  contract;
- only decisions that genuinely belong to bank business policy or runtime
  environment remain external, for example resend/cooldown policy,
  environment URLs, certificates, trust chains, SLA values and
  environment-specific operational limits.

D00-CB-OWNERSHIP does not implicitly approve unresolved business-policy or
runtime parameters.

## Status model

### Contract approval

- `APPROVED`: target contract approved by the required SIXPAY/Core Banking
  governance.
- `PENDING_APPROVAL`: target contract exists but one or more programme,
  business or Security decisions remain unresolved.
- `PENDING_BANK_APPROVAL`: remains valid for contracts that genuinely depend
  on approval of a pre-existing external bank/provider API. It is not used for
  Core Banking target-contract design decisions governed by D00-CB-OWNERSHIP.
- `TO_DEFINE`: no stable target contract exists.

### MVP requirement

- `REQUIRED`: required by the target MVP.
- `OPTIONAL`: used only if the bank confirms support and the project enables it.

`OPTIONAL` is not an approval status.

## Inventory

| Capability | Contract | Owner | Contract status | MVP |
|---|---|---|---|---|
| Customer discovery | `amplitude-customer-verification-api-v1.yaml` | Customer | `APPROVED` | `REQUIRED` |
| Customer/KYC/account verification | `amplitude-customer-verification-api-v1.yaml` | Customer | `APPROVED` | `REQUIRED` |
| Payment confirmation / OTP challenge | `amplitude-payment-confirmation-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |
| Atomic T0 financial execution: mandatory controls + Treasury resolution/use + customer debit + Treasury credit | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_APPROVAL` | `REQUIRED` |
| T0 financial outcome lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_APPROVAL` | `REQUIRED` |
| Reversal + reversal lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_APPROVAL` | `OPTIONAL — PENDING_PROGRAMME_ENABLEMENT` |
| T+1 accounting batch submission | physical provider contract `TO_DEFINE`; implementation shape already exists behind `AccountingBatchGateway` | Accounting | `TO_DEFINE` | `REQUIRED — API MVP` |
| T+1 accounting/TFJ result confirmation + fallback lookup | `amplitude-end-of-day-confirmation-api-v1.yaml` | Accounting / Payment lifecycle | `PENDING_APPROVAL` | `REQUIRED` |
| T+1 CSV/file accounting submission | contract `TO_DEFINE` | Accounting | `TO_DEFINE` | `DEFERRED_FUTURE` |

## Payment execution ordering

For the MVP Payment flow, Core Banking capabilities are ordered as follows:

```text
TRESOR PAY payment request
        ↓
SIXPAY durable Payment persistence
        ↓
RECEIVED
        ↓
Amplitude customer/account/KYC verification
        ↓
VERIFIED only
        ↓
Payment confirmation challenge / OTP
        ↓
successful customer confirmation
        ↓
later execution-time funds control and posting
```

The Customer Verification capability is a mandatory prerequisite of Payment
Confirmation. A `REJECTED` or `INDETERMINATE` customer/account/KYC verification
must not create or send an OTP challenge.

The canonical `customerReference` and debtor `accountReference` used by Payment
Confirmation come from the completed VERIFIED Amplitude verification result.
They are not inferred from NIU, TRESOR PAY subscription references or other
non-authoritative identifiers.

Customer Verification remains distinct from execution-time Funds Control.

After successful OTP/customer confirmation, the eight mandatory Funds Control
checks are evaluated inside the same protected synchronous T0 Core Banking
financial command that resolves/uses the configured Treasury destination,
debits the customer account and credits the Treasury account.

The MVP does not perform a standalone read-only funds check followed by a later
debit. Other banking channels may change the same account concurrently, so
financial availability must be decided by Core Banking at execution time.

The canonical detailed baseline is:
`documentation/architecture/integration/payment-financial-execution-and-accounting-eod-baseline.md`.

## Target Core Banking signatures

### Customer verification

`POST /api/v1/customer-verifications`

Customer discovery:
- `GET /api/v1/customers`
- `GET /api/v1/customers/{customerReference}`
- `GET /api/v1/customers/{customerReference}/accounts`

### T0 financial execution

The existing target path remains `POST /api/v1/payment-postings` until the
revised physical contract is approved. For the MVP this is one financial command,
not a prior `/payment-checks` call followed by a later posting call.

The command must cover all eight mandatory execution-time controls and, only if
all pass, the protected Treasury resolution/use plus customer debit and Treasury
credit.

A financial command with an uncertain transport outcome is never blindly retried.

### Posting lookup

- `GET /api/v1/payment-posting-lookups/{idempotencyKey}`
- `GET /api/v1/payment-postings/{bankPostingReference}`

At least one authoritative lookup mechanism is mandatory before posting sandbox certification.

### Reversal

- `POST /api/v1/payment-postings/{bankPostingReference}/reversals`
- `GET /api/v1/payment-postings/{bankPostingReference}/reversals/{reversalReference}`

Reversal is `OPTIONAL` until the programme explicitly enables it and its
business/operational prerequisites are approved.

### Accounting T+1 / TFJ / EOD

Accounting first constitutes a batch from financially successful Payments whose
TRESOR PAY status evidence satisfies the accounting-eligibility rule.

For the MVP, Accounting submits the payment batch through the Core Banking API
integration behind `AccountingBatchGateway`. Core Banking owns generation and
posting of the accounting entries. The final provider endpoint/path and complete
wire contract remain `TO_DEFINE` and must not be invented.

`amplitude-end-of-day-confirmation-api-v1.yaml` remains the result-confirmation /
reconciliation contract, not the batch-submission contract. Its revised target
semantics require review.

CSV/file submission is explicitly deferred until a separate approved file
contract exists.

## Resolved documentary drift

1. D00-CB-OWNERSHIP replaces the former provider-discovery approval model for
   Core Banking target contracts.
2. `OPTIONAL` describes MVP requirement, not contract approval.
3. `amplitude-payment-verification-api-v1` is obsolete/nonexistent;
   execution-time funds checking belongs to
   `amplitude-payment-posting-api-v1.yaml`.
4. `AmplitudeBankingClient` is legacy foundation debt; narrow
   capability-specific clients are the target.
5. Amplitude owns banking facts; SIXPAY owns SIXPAY Customer enrollment and
   partner-subscription lifecycle.
6. Environment URLs, certificates, trust material, SLA values and
   environment-specific limits remain external configuration and do not block
   approval of a stable logical target contract.

## Promotion to APPROVED

Promotion requires traceable programme approval of the target endpoint/method,
request/response schema, security profile, error/status model and
idempotency/recovery semantics where applicable.

Environment-specific URLs, certificates, trust chains, SLA values and
operational limits are validated during environment/sandbox certification and
are not prerequisites for approving the logical target contract unless they
change its semantics.
