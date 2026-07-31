# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.2 — Identifiers and Value Objects`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `VALUE_OBJECT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate decision

`Payment` remains the sole write Aggregate Root and represents one logical
TRESOR PAY payment intention for its entire lifecycle.

Its detailed identifier and Value Object rules are defined in:

`documentation/ai/payment/PAYMENT_VALUE_OBJECT_CATALOGUE.md`

## 2. Required state at creation

| Attribute | Type |
| --- | --- |
| `id` | `PaymentId` |
| `source` | `PaymentSource` |
| `externalPaymentReference` | `ExternalPaymentReference` |
| `externalSubscriptionReference` | `ExternalSubscriptionReference` |
| `publicPaymentReference` | `PublicPaymentReference` |
| `requestIdentity` | `PaymentRequestIdentity` |
| `financialInstitution` | `FinancialInstitutionCode` |
| `debtorAccount` | `DebtorAccountReference` |
| `requestedAmount` | shared-kernel `Money` |
| `treasuryAllocationIntent` | `TreasuryAllocationIntent` |
| `status` | `PaymentStatus` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |
| `businessVersion` | non-negative monotonic version |

## 3. Optional state

| Attribute | Type |
| --- | --- |
| `authorizationEvidence` | Lot 2.3 snapshot |
| `bankingVerificationEvidence` | Lot 2.3 snapshot |
| `fundsControlEvidence` | Lot 2.3 snapshot |
| `resolvedTreasuryAccount` | `TreasuryAccountReference` |
| `postingOutcome` | Lot 2.3 snapshot |
| `bankPostingReference` | `BankPostingReference` |
| `tfjConfirmation` | Lot 2.3 snapshot |
| `reversalOutcome` | Lot 2.3 snapshot |
| `failure` | current relevant `PaymentFailure` |
| `finalizedAt` | `Instant` |

## 4. Identity decisions

- `PaymentId`: non-nil UUID v4, generated outside aggregate;
- external uniqueness: `PaymentSource + ExternalPaymentReference`;
- MVP source: `TRESOR_PAY`;
- public reference: `PAY-` plus uppercase ULID;
- request identity: idempotency key + fingerprint + correlation ID;
- none of these identifiers may substitute for another.

## 5. Account decisions

`DebtorAccountReference` contains:

- bank code;
- opaque integration token;
- masked display;
- keyed account-binding fingerprint.

Payment never stores the clear account number.

`TreasuryAccountReference` is created only by protected bank configuration
resolution and cannot be created or overridden from TRESOR PAY input.

## 6. Allocation decision

`TreasuryAllocationIntent` is bounded to 1–20 unique beneficiary allocations.

Every amount is positive, uses the Payment currency and the exact sum equals
the requested Payment amount.

## 7. Failure decision

Payment stores at most one current relevant structured `PaymentFailure`.

Failure history remains append-only audit/reporting.

## 8. Structural invariants retained

All `PAY-AGG-001` to `PAY-AGG-014` invariants remain applicable.

Additional Value Object decisions are `PAY-DEC-IA1-007` to
`PAY-DEC-IA1-016`.

## 9. Still deferred

Snapshots, complete invariants, commands, events, policies and final validation
remain assigned to Lots 2.3–2.8.

## 10. Verdict

```text
AGGREGATE ROOT: PREPARED
IDENTIFIERS AND VALUE OBJECTS: PREPARED
CODE GENERATION: FORBIDDEN
```
