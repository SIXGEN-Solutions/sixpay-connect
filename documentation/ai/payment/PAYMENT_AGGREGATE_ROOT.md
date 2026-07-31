# SIXPAY CONNECT — Payment Aggregate Root

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.3 — Snapshots and Business Evidence`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `SNAPSHOT_MODEL_PREPARED`  
> **Code generation:** **FORBIDDEN**

## 1. Aggregate decision

`Payment` remains the sole write Aggregate Root for one logical TRESOR PAY
payment intention.

Normative supporting documents:

- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`.

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

## 3. Current optional decision evidence

| Attribute | Type |
| --- | --- |
| `authorizationEvidence` | `AuthorizationEvidenceSnapshot` |
| `bankingVerificationEvidence` | `BankingVerificationSnapshot` |
| `fundsControlEvidence` | `FundsControlSnapshot` |
| `treasuryResolutionEvidence` | `TreasuryAccountResolutionSnapshot` |
| `resolvedTreasuryAccount` | `TreasuryAccountReference` |
| `postingOutcome` | `PostingOutcomeSnapshot` |
| `bankPostingReference` | `BankPostingReference` |
| `tfjConfirmation` | `EndOfDayConfirmationSnapshot` |
| `reversal` | `ReversalSnapshot` |
| `failure` | current relevant `PaymentFailure` |
| `finalizedAt` | `Instant` |

Payment stores at most one current accepted snapshot per category. All previous
versions are persisted in append-only audit/reporting, not in the aggregate.

## 4. Snapshot acceptance

A snapshot enters Payment only through a named aggregate operation after:

- source authentication/validation outside the aggregate;
- canonical mapping;
- identity, bank, amount and account-binding verification;
- freshness validation when applicable;
- structural consistency validation;
- evidence replay/conflict detection.

The aggregate never receives an external DTO or raw provider payload.

## 5. Snapshot replacement

An identical replay is a no-op.

A different fingerprint under the same evidence identity is a conflict and
causes no Payment mutation.

A newer snapshot can replace the current one only when the lifecycle permits a
new observation and the new evidence is more authoritative or conclusive.
Terminal-state evidence is never silently replaced.

## 6. Financial evidence rules

- available balance is not stored;
- `UNKNOWN` posting outcome is not failure;
- confirmed financial effect requires principal posting reference;
- completed debit + CUT credit is not TFJ finality;
- only uniquely matched final TFJ evidence enters Payment;
- TFJ `PENDING`, unmatched or quarantined evidence remains in Accounting;
- reversal retains original Payment and posting identity.

## 7. Confidentiality

Full snapshots are never serialized automatically into domain events.

Events and query projections use explicit safe views defined by their own
contracts. Credentials, raw token/claims, KYC values, clear account data and
provider payloads remain forbidden.

## 8. Applicable decisions

- Aggregate decisions: `PAY-DEC-IA1-002` to `PAY-DEC-IA1-006`;
- Value Object decisions: `PAY-DEC-IA1-007` to `PAY-DEC-IA1-016`;
- Snapshot decisions: `PAY-DEC-IA1-017` to `PAY-DEC-IA1-026`;
- structural snapshot invariants: `PAY-SNAP-001` to `PAY-SNAP-018`.

## 9. Deferred scope

Complete cross-state invariants, commands, events, policies and final model
validation remain assigned to Lots 2.4–2.8.

## 10. Verdict

```text
AGGREGATE ROOT: PREPARED
IDENTIFIERS AND VALUE OBJECTS: PREPARED
SNAPSHOTS AND BUSINESS EVIDENCE: PREPARED
CODE GENERATION: FORBIDDEN
```
