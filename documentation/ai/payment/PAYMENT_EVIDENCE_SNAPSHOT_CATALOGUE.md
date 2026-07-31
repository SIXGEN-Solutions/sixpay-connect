# SIXPAY CONNECT — Payment Snapshots and Business Evidence Catalogue

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.6 — Domain Events`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `EVENT_SAFE_PROJECTIONS_BOUND`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This document specifies the immutable, minimized evidence retained by the
`Payment` Aggregate Root when an internal or external fact has directly
justified a Payment decision.

A snapshot proves:

- what canonical fact Payment accepted;
- which source produced or resolved it;
- when the source observed it;
- when Payment accepted it;
- which Payment identity and financial context it was bound to;
- whether the evidence was fresh and conclusive enough for the transition.

A snapshot does not reproduce an external payload and does not replace the
source system's current truth.

## 2. Snapshot principles

Every Payment snapshot:

1. is immutable;
2. uses value equality;
3. is bounded in size;
4. contains only decision-relevant facts;
5. contains no credential, raw token, full account number, raw KYC value,
   signature or external payload;
6. records source, observation time, acceptance time, correlation identity and
   a canonical evidence fingerprint;
7. is bound to the Payment identity, amount, bank or posting reference where
   applicable;
8. is created from a canonical internal result, never directly from an external
   DTO;
9. is accepted only by a named aggregate operation;
10. does not perform transport, lookup or cryptographic validation itself;
11. may be replaced only under the controlled rules in section 13;
12. has its full historical versions preserved outside the aggregate in
    append-only audit.

`acceptedAt` means that the aggregate accepted the snapshot as evidence for a
decision. It does not mean that the result was favorable.

## 3. Common evidence metadata

Every snapshot contains an `EvidenceMetadata` Value Object.

```text
EvidenceMetadata
├── sourceSystem
├── correlationId
├── observationChannel
├── evidenceFingerprint
├── observedAt
└── acceptedAt
```

### `sourceSystem`

Closed values reused from `ExternalSystem`:

```text
TRESOR_PAY
AMPLITUDE
SIXPAY
```

`NOT_APPLICABLE` is not permitted for a snapshot.

### `correlationId`

The correlation identity associated with the canonical observation.

A recovery lookup may use a later technical trace, but the canonical result
must remain linked to the original Payment and its business correlation.

### `observationChannel`

Closed values:

```text
LOCAL_VALIDATION
DIRECT_RESPONSE
IDEMPOTENCY_LOOKUP
BANK_REFERENCE_LOOKUP
ASYNC_CALLBACK
SCHEDULED_LOOKUP
PROTECTED_CONFIGURATION_RESOLUTION
```

### `evidenceFingerprint`

Format:

```text
v1:sha256:<64 lowercase hexadecimal characters>
```

The fingerprint is calculated outside the aggregate from the canonical,
minimized evidence representation. It excludes credentials, signatures, raw
tokens, raw external payloads and transport-only metadata.

The same evidence identity with another fingerprint is a conflict, not an
update.

### Temporal rules

- `observedAt` is the source or resolver observation time;
- `acceptedAt` is the instant Payment accepted the evidence;
- `acceptedAt` must not precede `observedAt`;
- all technical instants use UTC `Instant`;
- bank business dates use `LocalDate`.

**Decision:** `PAY-DEC-IA1-018`.

## 4. Snapshot inventory

| Snapshot | Aggregate presence | Source |
| --- | --- | --- |
| `AuthorizationEvidenceSnapshot` | Optional after authorization decision | TRESOR PAY evidence validated by SIXPAY |
| `BankingVerificationSnapshot` | Optional after customer/account verification | Amplitude through Customer |
| `FundsControlSnapshot` | Optional after execution checks | Amplitude through Accounting |
| `TreasuryAccountResolutionSnapshot` | Optional after protected CUT resolution | SIXPAY protected configuration |
| `PostingOutcomeSnapshot` | Optional after posting submission/lookup | Amplitude through Accounting |
| `EndOfDayConfirmationSnapshot` | Optional after unique TFJ match | Amplitude through Accounting |
| `ReversalSnapshot` | Optional after reversal authorization | SIXPAY + Amplitude |

Payment stores at most one current accepted snapshot per category. The complete
history belongs to audit and reporting.

**Decision:** `PAY-DEC-IA1-017`.

---

# 5. Authorization evidence

## 5.1 `AuthorizationEvidenceSnapshot`

### Semantics

Minimal proof of the signed TRESOR PAY authorization decision consumed by
Payment.

It proves that Security and Integration validated the authorization evidence
and its bindings without storing the JWT or raw claims.

### Shape

```text
AuthorizationEvidenceSnapshot
├── authorizationEvidenceReference
├── outcome
├── tokenFingerprint
├── issuer
├── keyId
├── signatureAlgorithm
├── scope
├── bindingResults
├── issuedAt
├── validFrom
├── expiresAt
├── rejectionCode?
└── metadata
```

### `authorizationEvidenceReference`

Versioned keyed digest of the JWT `jti`:

```text
v1:hmac-sha256:<64 lowercase hexadecimal characters>
```

The raw `jti` and HMAC key do not enter Payment.

### `outcome`

```text
APPROVED
REJECTED
```

Infrastructure/key availability failure without a valid decision is represented
by `PaymentFailure`, not a fabricated authorization snapshot.

### Retained bindings

`bindingResults` is a bounded set containing exactly the evaluated binding
types and their result:

```text
SUBSCRIPTION_REFERENCE
CLIENT_APPLICATION
CUSTOMER_IDENTITY
FINANCIAL_INSTITUTION
DEBTOR_ACCOUNT
EXTERNAL_PAYMENT_REFERENCE
PAYMENT_SCOPE
TOKEN_REPLAY
```

Each result is:

```text
MATCH
MISMATCH
NOT_EVALUATED
```

No customer NIU, account number, JWT claim value or credential is retained.

### Validation

For `APPROVED`:

- every mandatory binding is `MATCH`;
- scope is `payment:initiate`;
- issuer is the approved TRESOR PAY issuer;
- algorithm is in the approved asymmetric set;
- `acceptedAt` is inside the token validity interval;
- `rejectionCode` is absent.

For `REJECTED`:

- at least one binding is `MISMATCH`, or token validity/signature semantics
  produced a stable security rejection;
- a stable safe `rejectionCode` is required;
- no banking call is authorized.

### Confidentiality

`RESTRICTED_SECURITY_EVIDENCE`.

Allowed in aggregate/audit:

- evidence reference;
- token fingerprint;
- key identifier;
- algorithm;
- safe binding results;
- safe rejection code.

Forbidden:

- raw JWT;
- raw `jti`;
- claims containing customer or account values;
- signature bytes;
- Subscription Key;
- JWKS body.

### Lifecycle

One accepted authorization decision per Payment. An identical replay is a
no-op. A different fingerprint for the same evidence reference is a security
conflict.

**Decision:** `PAY-DEC-IA1-019`.

---

# 6. Banking verification evidence

## 6.1 `BankingVerificationSnapshot`

### Semantics

Minimal canonical proof of the fresh Amplitude customer, KYC and debtor-account
verification that Payment used.

### Shape

```text
BankingVerificationSnapshot
├── verificationId
├── outcome
├── accountBindingFingerprint
├── checks
└── metadata
```

### `verificationId`

Non-nil UUID assigned by the canonical Amplitude verification result.

### `outcome`

```text
VERIFIED
REJECTED
INDETERMINATE
```

### `checks`

Bounded unique set of `BankingVerificationCheckEvidence`:

```text
type
result
reasonCode?
checkedAt?
```

Permitted types:

```text
CUSTOMER_EXISTS
FINANCIAL_INSTITUTION_MATCHES
NIU_MATCHES
IDENTITY_MATCHES
ACCOUNT_EXISTS
ACCOUNT_BELONGS_TO_CUSTOMER
ACCOUNT_IS_ACTIVE
ACCOUNT_NOT_BLOCKED
ACCOUNT_NOT_OPPOSED
REQUIRED_KYC_PRESENT
REQUIRED_KYC_VERIFIED
```

Results:

```text
PASS
FAIL
UNKNOWN
```

The canonical order is the order above, independent of provider array order.

### Validation matrix

- `VERIFIED`: every applicable mandatory check is present and `PASS`;
- `REJECTED`: at least one decisive check is `FAIL`;
- `INDETERMINATE`: the evidence does not permit approval or definitive business
  rejection;
- duplicate check types are forbidden;
- `accountBindingFingerprint` must equal the aggregate debtor-account binding
  fingerprint;
- source must be `AMPLITUDE`.

### Explicitly excluded

Payment does not retain:

- customer name;
- NIU;
- phone number;
- email;
- KYC values;
- raw customer reference;
- raw account reference;
- account restrictions array;
- full Amplitude identity/account objects.

Those facts remain with Amplitude or the `ObservedCustomer` projection.

### Lifecycle

The aggregate retains the snapshot that directly justified its current
banking-verification decision. Later verification cycles do not rewrite a
terminal rejected Payment.

**Decision:** `PAY-DEC-IA1-020`.

---

# 7. Funds-control evidence

## 7.1 `FundsControlSnapshot`

### Semantics

Minimal proof of the fresh read-only execution checks performed for the exact
Payment amount, account and currency before posting.

### Shape

```text
FundsControlSnapshot
├── verificationReference
├── outcome
├── checkedAmount
├── accountBindingFingerprint
├── checks
├── validUntil
└── metadata
```

### `verificationReference`

Opaque Amplitude bank reference, 8–128 canonical characters.

### `outcome`

```text
VERIFIED
REJECTED
INDETERMINATE
```

### `checks`

Bounded unique set of `FundsControlCheckEvidence`:

```text
ACCOUNT_EXISTS
ACCOUNT_ACTIVE
DEBIT_ALLOWED
CURRENCY_SUPPORTED
AVAILABLE_FUNDS_SUFFICIENT
PER_TRANSACTION_LIMIT_NOT_EXCEEDED
DAILY_LIMIT_NOT_EXCEEDED
OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED
```

Each check contains:

```text
type
result: PASS | FAIL | UNKNOWN
reasonCode?
checkedAt
```

### Validation

For `VERIFIED`:

- every mandatory check is present and `PASS`;
- `checkedAmount` equals the aggregate requested amount;
- the account fingerprint matches;
- `acceptedAt <= validUntil`;
- source is `AMPLITUDE`.

For `REJECTED`:

- at least one decisive check is `FAIL`;
- no posting is authorized.

For `INDETERMINATE`:

- no posting is authorized;
- the result may drive recovery, failure or a controlled new read.

### Available amount minimization

The optional `availableAmount` from Amplitude is not stored in Payment.

Payment needs the decision that funds were sufficient for its exact amount, not
the customer's balance.

### Lifecycle

A favorable snapshot is frozen once posting is authorized. A new funds check
requires a new verification reference and is accepted only before financial
submission.

**Decision:** `PAY-DEC-IA1-021`.

---

# 8. Treasury configuration evidence

## 8.1 `TreasuryAccountResolutionSnapshot`

### Semantics

Proof that the CUT/Treasury account used for posting was resolved from
protected bank-controlled configuration and was coherent with the Payment bank
and allocation intent.

### Shape

```text
TreasuryAccountResolutionSnapshot
├── treasuryAccountReference
├── allocationIntentFingerprint
├── resolutionOutcome
├── resolverPolicyVersion
├── rejectionCode?
└── metadata
```

### `resolutionOutcome`

```text
RESOLVED
REJECTED
```

### Rules

For `RESOLVED`:

- source is `SIXPAY`;
- channel is `PROTECTED_CONFIGURATION_RESOLUTION`;
- resolved bank equals the aggregate `FinancialInstitutionCode`;
- configuration identifier and version are present;
- allocation fingerprint corresponds to the canonical
  `TreasuryAllocationIntent`;
- no inbound TRESOR PAY account value created or replaced the protected
  reference;
- `rejectionCode` is absent.

For `REJECTED`:

- a stable safe reason code is required;
- posting cannot be authorized.

### Confidentiality

The snapshot may retain the protected configuration identifier/version and
masked account representation. It does not expose the account token in domain
events.

**Decision:** `PAY-DEC-IA1-022`.

---

# 9. Posting evidence

## 9.1 `PostingOutcomeSnapshot`

### Semantics

Canonical evidence of the current authoritative outcome of one idempotent
Amplitude posting instruction.

### Shape

```text
PostingOutcomeSnapshot
├── postingInstructionId
├── postingCommandIdempotencyKey
├── outcome
├── bankPostingReference?
├── debitLeg
├── cutCreditLeg
├── amount
├── businessDate?
├── rejectionCode?
├── nextAction
└── metadata
```

### `outcome`

```text
COMPLETED
REJECTED_NO_FINANCIAL_EFFECT
DEBIT_CONFIRMED_CUT_CREDIT_PENDING
REVERSAL_REQUIRED
UNKNOWN
```

### `PostingLegEvidence`

```text
status
bankEntryReference?
effectiveAt?
failureCode?
```

Statuses:

```text
NOT_STARTED
PENDING
SUCCEEDED
FAILED
UNKNOWN
```

### `nextAction`

```text
NONE
QUERY_OUTCOME
WAIT_FOR_CUT_CREDIT
OPEN_RECONCILIATION
REQUEST_EXPLICIT_REVERSAL
```

### Structural consistency

#### `COMPLETED`

- debit leg is `SUCCEEDED`;
- CUT-credit leg is `SUCCEEDED`;
- principal bank posting reference is present;
- amount equals Payment amount;
- `nextAction = NONE`;
- does not establish TFJ finality.

#### `REJECTED_NO_FINANCIAL_EFFECT`

- no leg is `SUCCEEDED`, `PENDING` or `UNKNOWN`;
- stable rejection code is present;
- `nextAction = NONE`;
- absence of financial effect is explicit.

#### `DEBIT_CONFIRMED_CUT_CREDIT_PENDING`

- debit leg is `SUCCEEDED`;
- CUT-credit leg is `PENDING` or `UNKNOWN`;
- principal posting reference is present;
- next action is `WAIT_FOR_CUT_CREDIT`, `QUERY_OUTCOME` or
  `OPEN_RECONCILIATION`.

#### `REVERSAL_REQUIRED`

- at least one confirmed or authoritatively reconciled financial effect exists;
- principal posting reference is present;
- `nextAction = REQUEST_EXPLICIT_REVERSAL`.

#### `UNKNOWN`

- outcome is neither success nor failure;
- `nextAction = QUERY_OUTCOME` or `OPEN_RECONCILIATION`;
- blind financial resubmission is forbidden;
- a bank reference may be absent or present.

### Reference consistency

When `bankPostingReference` exists in both the snapshot and aggregate, they must
be equal.

Leg references in `BankPostingReference` must match the corresponding
`bankEntryReference` values when both are present.

### Observation channels

- initial response: `DIRECT_RESPONSE`;
- lookup by original command key: `IDEMPOTENCY_LOOKUP`;
- lookup by principal reference: `BANK_REFERENCE_LOOKUP`.

### Replacement rule

An authoritative lookup may replace an `UNKNOWN` or incomplete snapshot only
when:

- posting instruction ID and command idempotency key are unchanged;
- Payment, amount and account bindings are unchanged;
- new evidence identity/fingerprint passes conflict checks;
- the new result is more conclusive or explicitly corrects the current
  authoritative result.

Conflicting conclusive financial results are quarantined and require manual
reconciliation; they are not silently overwritten.

**Decision:** `PAY-DEC-IA1-023`.

---

# 10. TFJ evidence

## 10.1 `EndOfDayConfirmationSnapshot`

### Semantics

Minimal proof of a final or failed Amplitude TFJ result that Accounting has
authenticated, durably persisted and uniquely matched to this Payment.

An unmatched, ambiguous, conflicting or merely pending result never enters the
Payment aggregate.

### Shape

```text
EndOfDayConfirmationSnapshot
├── confirmationId
├── financialInstitutionCode
├── businessDate
├── publicPaymentReference
├── principalBankPostingReference
├── tfjBatchReference?
├── tfjStatus
├── failureEvidence?
├── confirmedAt
├── matchedAt
└── metadata
```

### `tfjStatus`

Only these values may enter Payment:

```text
INTEGRATED
FAILED
```

Provider status `PENDING` remains in Accounting/reconciliation and does not
replace Payment evidence.

### Matching proof

The snapshot repeats the four authoritative match keys:

```text
financialInstitutionCode
businessDate
publicPaymentReference
principalBankPostingReference
```

They must equal the aggregate context.

### `TfjFailureEvidence`

Required only for `FAILED`:

```text
code
recoveryAction
```

Recovery actions:

```text
MANUAL_RECONCILIATION
REVERSAL_REVIEW
REVERSAL_REQUIRED
```

The provider's free-form description is kept in protected audit when safe, not
in the aggregate.

### Validation

For `INTEGRATED`:

- no failure evidence;
- unique matching was successful;
- source is `AMPLITUDE`;
- channel is `ASYNC_CALLBACK` or `SCHEDULED_LOOKUP`;
- `matchedAt >= confirmedAt`;
- acceptance establishes Treasury finality.

For `FAILED`:

- failure evidence is present;
- final Payment transition depends on Lot 2.4/2.5 rules;
- delay alone is not represented as `FAILED`.

### Replay/conflict

- same confirmation ID and fingerprint: no-op;
- same match/idempotency identity with different fingerprint: quarantine;
- a successful matched finality cannot be replaced by a later conflicting
  callback inside Payment.

**Decision:** `PAY-DEC-IA1-024`.

---

# 11. Reversal evidence

## 11.1 `ReversalSnapshot`

### Semantics

Current bounded proof of one explicitly authorized reversal process attached to
the original Payment and original posting.

### Shape

```text
ReversalSnapshot
├── originalBankPostingReference
├── reversalInstructionId
├── reversalCommandIdempotencyKey
├── authorization
└── outcome?
```

## 11.2 `ReversalAuthorizationEvidence`

```text
authorizationType
authorizationReference
requestedBySubject
reasonCode
authorizedAt
requestedAt
```

Authorization types:

```text
BANK_INSTRUCTION
APPROVED_RUNBOOK
```

Rules:

- authorization reference is immutable and auditable;
- requested-by subject is an opaque service/operator subject, never a
  credential;
- authorization exists before reversal submission;
- original posting reference is never replaced.

## 11.3 `ReversalOutcomeEvidence`

```text
reversalReference?
outcome
reversalEntryReference?
reasonCode?
metadata
```

Outcomes:

```text
REVERSED
REJECTED
NOT_ALLOWED
UNKNOWN
```

Validation:

- `REVERSED`: reversal reference is required;
- `REJECTED` or `NOT_ALLOWED`: stable reason code required;
- `UNKNOWN`: authoritative lookup required; blind replay forbidden;
- source is `AMPLITUDE`;
- observation channel is `DIRECT_RESPONSE` or `BANK_REFERENCE_LOOKUP`.

### Lifecycle

At reversal submission, `outcome` may be absent.

A later result creates a new immutable `ReversalSnapshot` with the same:

- original posting reference;
- reversal instruction ID and command idempotency key;
- authorization evidence.

The aggregate replaces its current snapshot atomically. Audit retains every
version.

**Decision:** `PAY-DEC-IA1-025`.

---

# 12. Evidence deliberately kept outside Payment

The following are not aggregate snapshots:

| Evidence | Owner |
| --- | --- |
| Raw inbound TRESOR PAY request | Integration journal under retention policy |
| JWT/JWS, signature or JWKS | Security/Integration, never persisted as Payment data |
| Full Amplitude customer/KYC payload | Customer/Integration protected processing |
| Current balance or available amount | Amplitude |
| HTTP request/response bodies | Integration journal only when approved and minimized |
| Retry attempts and circuit-breaker state | Integration/Operations |
| Notification delivery attempts | Notification |
| Unmatched TFJ confirmations | Accounting quarantine |
| Full transition and snapshot history | Append-only audit/reporting |
| Operator comments and attachments | Operations case management |
| Provider free-form technical error | Protected integration/audit journal |

## 13. Snapshot acceptance and replacement policy

### 13.1 Acceptance

A named Payment operation accepts a snapshot only when:

1. the aggregate is in an eligible state;
2. the snapshot source and channel are allowed;
3. identity, bank, amount and account bindings match;
4. timestamps are structurally valid;
5. freshness rules are satisfied where required;
6. the evidence fingerprint is valid;
7. the result combination is internally consistent;
8. no conflicting evidence identity already exists.

### 13.2 Identical replay

Same evidence identity and same fingerprint:

```text
NO_OP
NO_NEW_DOMAIN_TRANSITION
NO_NEW_BUSINESS_EVENT
AUDIT_REPLAY_IF_REQUIRED
```

### 13.3 Evidence conflict

Same evidence identity or idempotency scope with a different fingerprint:

```text
REJECT_CONFLICT
DO_NOT_MUTATE_PAYMENT
QUARANTINE_WHEN_EXTERNAL_FINANCIAL_OR_TFJ
AUDIT_AND_ALERT
```

### 13.4 Replacement

A current snapshot may be replaced only when:

- the lifecycle explicitly supports a new observation;
- immutable Payment bindings remain equal;
- replacement is more authoritative or more conclusive;
- the previous and replacement versions are preserved in audit;
- the transition is atomic with state, audit and Outbox intent.

### 13.5 Terminal evidence

Evidence supporting a terminal state is immutable inside Payment.

A later contradictory external result is an operational conflict and never
silently reopens or rewrites the terminal Payment.

**Decision:** `PAY-DEC-IA1-026`.

## 14. Snapshot confidentiality matrix

| Snapshot | Classification | Event exposure |
| --- | --- | --- |
| Authorization | `RESTRICTED_SECURITY_EVIDENCE` | Outcome, safe code, evidence reference only when justified |
| Banking verification | `RESTRICTED_BANKING_EVIDENCE` | Outcome and safe reason codes; no KYC/account reference |
| Funds control | `RESTRICTED_FINANCIAL_EVIDENCE` | Outcome and check types; no available balance |
| Treasury resolution | `SENSITIVE_BANK_CONFIGURATION_EVIDENCE` | Protected configuration ID/version only |
| Posting outcome | `RESTRICTED_FINANCIAL_EVIDENCE` | Safe outcome, posting refs and leg statuses according to contract |
| TFJ confirmation | `RESTRICTED_TREASURY_EVIDENCE` | Final status and safe references |
| Reversal | `RESTRICTED_FINANCIAL_EVIDENCE` | Safe outcome and references; no credential/operator detail |

Domain events use explicit safe payloads defined in Lot 2.6. They never
serialize the full snapshot by default.

## 15. Snapshot structural invariants

| ID | Invariant |
| --- | --- |
| `PAY-SNAP-001` | Every snapshot is immutable, bounded and value-equal. |
| `PAY-SNAP-002` | Every snapshot has source, correlation, channel, fingerprint, observedAt and acceptedAt. |
| `PAY-SNAP-003` | `acceptedAt` never precedes `observedAt`. |
| `PAY-SNAP-004` | Raw external payloads and credentials never enter a snapshot. |
| `PAY-SNAP-005` | Same evidence identity plus different fingerprint is a conflict. |
| `PAY-SNAP-006` | Identical evidence replay produces no second domain transition. |
| `PAY-SNAP-007` | Snapshot identity, bank, account and amount bindings must match Payment. |
| `PAY-SNAP-008` | Favorable stale funds evidence cannot authorize posting. |
| `PAY-SNAP-009` | Available account balance is not stored in Payment. |
| `PAY-SNAP-010` | Posting `UNKNOWN` never means failure and never authorizes blind replay. |
| `PAY-SNAP-011` | Confirmed financial-effect outcomes require a principal posting reference. |
| `PAY-SNAP-012` | `COMPLETED` posting does not establish TFJ finality. |
| `PAY-SNAP-013` | Only uniquely matched `INTEGRATED` TFJ evidence establishes Treasury finality. |
| `PAY-SNAP-014` | Pending/unmatched TFJ evidence remains outside Payment. |
| `PAY-SNAP-015` | Reversal evidence preserves original Payment and posting identities. |
| `PAY-SNAP-016` | Full snapshot history remains outside the aggregate. |
| `PAY-SNAP-017` | Terminal-state evidence is not silently replaced. |
| `PAY-SNAP-018` | Full snapshots are not serialized into domain events by default. |

Detailed cross-state invariants are finalized in Lot 2.4.

## 16. Decisions closed in Lot 2.3

| Decision ID | Decision |
| --- | --- |
| `PAY-DEC-IA1-017` | Payment stores one current accepted snapshot per evidence category; history is audit/reporting. |
| `PAY-DEC-IA1-018` | All snapshots use common source/correlation/channel/fingerprint/time metadata. |
| `PAY-DEC-IA1-019` | Authorization evidence stores minimized validation/binding proof, never JWT or raw claims. |
| `PAY-DEC-IA1-020` | Banking verification stores outcome and canonical checks, never identity/KYC payloads. |
| `PAY-DEC-IA1-021` | Funds evidence binds exact amount/account and excludes available balance. |
| `PAY-DEC-IA1-022` | Treasury resolution proof records protected configuration resolution. |
| `PAY-DEC-IA1-023` | Posting snapshot preserves command identity, outcome, leg evidence and controlled lookup resolution. |
| `PAY-DEC-IA1-024` | Only uniquely matched final TFJ evidence enters Payment; PENDING/unmatched stays in Accounting. |
| `PAY-DEC-IA1-025` | Reversal snapshot combines immutable authorization with optional canonical outcome. |
| `PAY-DEC-IA1-026` | Snapshot replay, conflict and replacement rules are explicit and auditable. |

## 17. Deferred to later lots

- complete cross-object and state invariants: Lot 2.4;
- exact aggregate commands and methods: Lot 2.5;
- event names and safe payloads: Lot 2.6;
- freshness, resolver and matching policies: Lot 2.7;
- final consistency validation: Lot 2.8.

## 18. Exit checklist

- [ ] Every decision-relevant external stage has a defined snapshot.
- [ ] Every snapshot has exact fields, source and timing rules.
- [ ] Raw credentials, KYC values and account data are excluded.
- [ ] Favorable, negative and indeterminate outcomes are distinguishable.
- [ ] Posting partial and unknown outcomes are representable.
- [ ] TFJ unique matching and finality rules are explicit.
- [ ] Reversal authorization and outcome are distinct but linked.
- [ ] Replay, conflict and replacement behavior is explicit.
- [ ] Aggregate history remains bounded.
- [ ] Code generation remains forbidden.

## 19. Verdict

```text
IA-1 LOT 2.3 SNAPSHOTS AND BUSINESS EVIDENCE PREPARED
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.7 — POLICIES AND DOMAIN SERVICES
CODE GENERATION: FORBIDDEN
```
