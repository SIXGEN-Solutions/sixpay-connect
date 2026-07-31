# SIXPAY CONNECT — Payment Invariant Catalogue

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Current lot:** `2.7 — Policies and Domain Services`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `POLICY_AND_SERVICE_BINDINGS_ADDED`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This catalogue closes the cross-object, lifecycle, financial-safety,
confidentiality, replay, concurrency and transaction invariants of the
`Payment` Aggregate Root.

An invariant is a condition that must remain true before and after every
successful aggregate mutation and after reconstitution.

The catalogue does not finalize command signatures or event payloads. Those are
defined in Lots 2.5 and 2.6.

## 2. Authority and compatibility

The invariants consolidate:

- `PAYMENT_AGGREGATE_ROOT.md`;
- `PAYMENT_VALUE_OBJECT_CATALOGUE.md`;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md`;
- Payment contracts;
- the IA-0P state machine, acceptance scenarios, security and resilience
  baselines.

Where the IA-0P state machine conflicts with IA-1 decisions, this catalogue is
authoritative for the following semantics:

1. banking verification and funds control are separate evidence gates;
2. notification intent is orthogonal to financial state and delivery never
   gates TFJ processing;
3. `FAILED` requires proven absence of financial effect;
4. `REJECTED` is a pre-financial conclusive rejection;
5. `DEBITED` is valid only for a confirmed partial financial outcome;
6. TFJ finality requires a uniquely matched `INTEGRATED` result.

`PAYMENT_STATE_MACHINE.yaml` remains unchanged in Lot 2.4 and will be reconciled
with the final operations in Lot 2.5.

**Decision:** `PAY-DEC-IA1-027`.

## 3. Invariant types

| Enforcement | Meaning |
| --- | --- |
| `VALUE_OBJECT` | Enforced by construction of an immutable Value Object |
| `DOMAIN` | Enforced inside the Aggregate Root |
| `APPLICATION` | Enforced by use-case orchestration |
| `PERSISTENCE` | Enforced by transaction, versioning or uniqueness constraints |
| `INTEGRATION` | Enforced by canonical mapping and external-contract boundary |
| `POLICY` | Requires approved configuration, freshness or authorization policy |
| `ARCHITECTURE` | Enforced by module/dependency tests |
| `ACCOUNTING` | Enforced by Accounting matching/reconciliation before Payment consumption |
| `CONTRACT` | Enforced by approved API/event schemas |

## 4. Violation rule

Unless an invariant explicitly defines conflict or quarantine behavior:

```text
INVARIANT VIOLATION
→ reject the operation
→ leave Payment unchanged
→ do not increment businessVersion
→ do not register a new domain event
→ do not append a state-transition audit
→ do not create a new Outbox intent
```

Externally supplied conflicting financial or TFJ evidence is quarantined by the
owning module and does not mutate Payment.

**Decision:** `PAY-DEC-IA1-032`.

## Identity, creation and immutable intent
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-001` | A Payment has exactly one non-null, non-nil and immutable PaymentId for its entire lifecycle. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-002` | PaymentSource and ExternalPaymentReference are required at creation, immutable and never recycled. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-003` | The business uniqueness key is PaymentSource plus ExternalPaymentReference; repository-wide uniqueness is enforced atomically outside the aggregate. | `APPLICATION`, `PERSISTENCE` | `RETURN_EXISTING_OR_CONFLICT` | `APPLICATION`, `POSTGRES_INTEGRATION`, `CONCURRENCY` |
| `PAY-INV-004` | PublicPaymentReference is required, globally unique in SIXPAY, immutable and distinct from every internal, external, idempotency, correlation and bank reference. | `DOMAIN`, `PERSISTENCE` | `REJECT_OR_UNIQUE_CONSTRAINT` | `DOMAIN_UNIT`, `POSTGRES_INTEGRATION` |
| `PAY-INV-005` | ExternalSubscriptionReference is required for the MVP, immutable and never resolved or mutated as a local Subscription aggregate. | `DOMAIN`, `ARCHITECTURE` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `ARCHITECTURE_TEST` |
| `PAY-INV-006` | The original PaymentRequestIdentity is required at creation and its idempotency key, request fingerprint and correlation identifier never change. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-007` | FinancialInstitutionCode, DebtorAccountReference, requested Money and TreasuryAllocationIntent are immutable original-intent facts. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-008` | Requested Money and every Treasury allocation are strictly positive, use one currency and allocations sum exactly to the requested amount. | `VALUE_OBJECT`, `DOMAIN` | `REJECT_CREATION` | `VALUE_OBJECT_UNIT`, `PROPERTY_BASED` |
| `PAY-INV-009` | createdAt is immutable; updatedAt never precedes createdAt and changes only on a successful aggregate mutation. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-010` | businessVersion is non-negative and increases exactly once for each successful state-changing mutation; no-op replay and rejected operations do not increase it. | `DOMAIN`, `PERSISTENCE` | `STALE_OR_NO_MUTATION` | `DOMAIN_UNIT`, `POSTGRES_INTEGRATION`, `CONCURRENCY` |

## Domain boundary and confidentiality
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-011` | Payment domain state never contains a raw JWT, Subscription Key, credential, signature, JWKS body or secret. | `DOMAIN`, `INTEGRATION`, `PERSISTENCE` | `SECURITY_REJECTION` | `DOMAIN_UNIT`, `SERIALIZATION_TEST`, `SECURITY_TEST` |
| `PAY-INV-012` | Payment never stores a clear debtor or Treasury account number; protected token, mask and keyed fingerprint remain distinct representations. | `DOMAIN`, `INTEGRATION`, `PERSISTENCE` | `SECURITY_REJECTION` | `DOMAIN_UNIT`, `PERSISTENCE_ENCRYPTION`, `SECURITY_TEST` |
| `PAY-INV-013` | Raw TRESOR PAY, Amplitude, TFJ or notification payloads never enter the aggregate or domain events. | `DOMAIN`, `INTEGRATION` | `REJECT_MAPPING` | `ARCHITECTURE_TEST`, `MAPPING_TEST`, `EVENT_SCHEMA_TEST` |
| `PAY-INV-014` | Payment domain types have no dependency on Spring, Jakarta Persistence, Hibernate, Jackson, HTTP, Kafka, database entities or another business-domain implementation. | `ARCHITECTURE` | `BUILD_OR_ARCHITECTURE_FAILURE` | `ARCHITECTURE_TEST` |
| `PAY-INV-015` | Payment retains at most one current accepted snapshot per evidence category and one current relevant PaymentFailure; full histories remain in audit/reporting. | `DOMAIN` | `REJECT_RECONSTITUTION_OR_MUTATION` | `DOMAIN_UNIT`, `RECONSTITUTION_TEST` |
| `PAY-INV-016` | Domain events and external errors expose only explicitly approved safe projections and never serialize complete protected Value Objects or snapshots by default. | `DOMAIN`, `APPLICATION`, `CONTRACT` | `SCHEMA_OR_SECURITY_FAILURE` | `EVENT_SCHEMA_TEST`, `API_CONTRACT_TEST`, `SECURITY_TEST` |
| `PAY-INV-017` | Payment does not own transport retry, circuit-breaker state, notification delivery attempts, unmatched TFJ records or generic Outbox mechanics. | `ARCHITECTURE` | `ARCHITECTURE_FAILURE` | `ARCHITECTURE_TEST` |
| `PAY-INV-018` | Every safe message, reason code and audit projection is free of credentials, clear account data, raw KYC values, stack traces and raw provider errors. | `DOMAIN`, `APPLICATION`, `INTEGRATION` | `SECURITY_REJECTION` | `DOMAIN_UNIT`, `SECURITY_TEST`, `API_CONTRACT_TEST` |

## Admission and TRESOR PAY authorization
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-019` | An unauthenticated transport request creates no Payment; a Payment is durably created before any Amplitude business call. | `INTEGRATION`, `APPLICATION`, `PERSISTENCE` | `REJECT_BEFORE_CREATION` | `API_SECURITY_TEST`, `APPLICATION`, `POSTGRES_INTEGRATION` |
| `PAY-INV-020` | Payment creation requires all mandatory canonical identifiers, positive Money, protected debtor account and balanced Treasury allocation intent. | `DOMAIN` | `REJECT_CREATION_NO_EVENT` | `DOMAIN_UNIT`, `PROPERTY_BASED` |
| `PAY-INV-021` | Banking verification cannot begin before Payment has accepted an AuthorizationEvidenceSnapshot with outcome APPROVED. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-022` | An approved authorization snapshot requires every mandatory request binding to MATCH, a valid payment scope and acceptance inside the approved validity interval. | `DOMAIN`, `POLICY` | `SECURITY_REJECTION` | `DOMAIN_UNIT`, `POLICY_TEST`, `SECURITY_TEST` |
| `PAY-INV-023` | A rejected authorization snapshot forbids all Amplitude customer, funds and posting operations for that Payment. | `DOMAIN`, `APPLICATION` | `TERMINAL_REJECTION` | `DOMAIN_UNIT`, `APPLICATION` |
| `PAY-INV-024` | Authorization infrastructure failure without a canonical decision is represented as a technical PaymentFailure and never fabricated as APPROVED or REJECTED evidence. | `APPLICATION`, `DOMAIN` | `SAFE_FAILURE_OR_RECOVERY` | `APPLICATION`, `DOMAIN_UNIT` |
| `PAY-INV-025` | An identical authorization evidence replay is a no-op; the same authorization evidence identity with another fingerprint is a security conflict. | `DOMAIN`, `APPLICATION` | `NO_OP_OR_SECURITY_CONFLICT` | `DOMAIN_UNIT`, `REPLAY_TEST` |
| `PAY-INV-026` | Payment never infers, manages or changes a TRESOR PAY Subscription status from ExternalSubscriptionReference. | `DOMAIN`, `ARCHITECTURE` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `ARCHITECTURE_TEST` |

## Banking verification, funds and Treasury resolution
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-027` | BankingVerificationSnapshot and FundsControlSnapshot are distinct evidence categories and neither substitutes for the other. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-028` | Only a BankingVerificationSnapshot with outcome VERIFIED may permit progression to funds control. | `DOMAIN` | `REJECT_OR_REMAIN_CHECKING` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-029` | A banking verification outcome REJECTED produces a definitive business rejection before financial submission; INDETERMINATE never permits approval. | `DOMAIN` | `REJECT_OR_RECOVERY` | `DOMAIN_UNIT` |
| `PAY-INV-030` | Every accepted banking verification must match the aggregate institution and debtor-account binding fingerprint. | `DOMAIN` | `INTEGRATION_CONFLICT` | `DOMAIN_UNIT`, `MAPPING_TEST` |
| `PAY-INV-031` | ObservedCustomer or any read projection can inform operations but can never authorize Payment progression or replace a fresh Amplitude verification. | `DOMAIN`, `APPLICATION` | `REJECT_NO_MUTATION` | `APPLICATION`, `ARCHITECTURE_TEST` |
| `PAY-INV-032` | Only a FundsControlSnapshot with outcome VERIFIED, exact Payment amount/account binding and unexpired validity may authorize posting. | `DOMAIN`, `POLICY` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `POLICY_TEST` |
| `PAY-INV-033` | Funds outcome REJECTED forbids posting and supports business rejection; INDETERMINATE supports only controlled recovery or technical failure. | `DOMAIN` | `REJECT_OR_RECOVERY` | `DOMAIN_UNIT` |
| `PAY-INV-034` | Available balance and provider-disclosed available amount are never retained in Payment. | `DOMAIN`, `INTEGRATION` | `REJECT_MAPPING` | `MAPPING_TEST`, `SECURITY_TEST` |
| `PAY-INV-035` | Posting authorization requires a TreasuryAccountResolutionSnapshot with outcome RESOLVED and a TreasuryAccountReference produced from protected bank configuration. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT` |
| `PAY-INV-036` | Resolved Treasury institution and allocation-intent fingerprint must match the Payment; inbound beneficiary/account data cannot create or override the CUT reference. | `DOMAIN`, `POLICY` | `INTEGRATION_CONFLICT` | `DOMAIN_UNIT`, `POLICY_TEST` |
| `PAY-INV-037` | Once posting is authorized, the accepted favorable authorization, banking, funds and Treasury-resolution evidence cannot be replaced by a less authoritative, stale or differently bound result. | `DOMAIN` | `REJECT_CONFLICT` | `DOMAIN_UNIT`, `REPLAY_TEST` |
| `PAY-INV-038` | A Payment is approved for financial submission only when authorization, banking verification, funds control and Treasury resolution are all favorable and mutually coherent. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |

## Posting and financial safety
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-039` | One Payment authorizes at most one logical posting instruction for its original payment intention. | `DOMAIN`, `APPLICATION`, `PERSISTENCE` | `REJECT_SECOND_LOGICAL_POSTING` | `DOMAIN_UNIT`, `APPLICATION`, `POSTGRES_INTEGRATION` |
| `PAY-INV-040` | Every retry of the same posting instruction reuses the exact posting idempotency key and immutable instruction fingerprint; a new key is not a retry. | `APPLICATION`, `INTEGRATION`, `PERSISTENCE` | `IDEMPOTENCY_CONFLICT` | `APPLICATION`, `CONTRACT_TEST`, `CONCURRENCY` |
| `PAY-INV-041` | A posting outcome must match the original Payment reference, amount, institution, debtor account, Treasury reference and posting command identity. | `DOMAIN` | `INTEGRATION_CONFLICT` | `DOMAIN_UNIT`, `MAPPING_TEST` |
| `PAY-INV-042` | Posting outcome UNKNOWN means neither success nor failure, forbids blind financial resubmission and requires authoritative lookup or reconciliation. | `DOMAIN`, `APPLICATION` | `ACCOUNTING_OUTCOME_UNKNOWN` | `DOMAIN_UNIT`, `RESILIENCE_TEST` |
| `PAY-INV-043` | A posting may be classified REJECTED_NO_FINANCIAL_EFFECT or Payment FAILED only when authoritative evidence proves that neither debit nor CUT credit occurred. | `DOMAIN` | `REJECT_INVALID_FAILURE_CLASSIFICATION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-044` | Posting outcome COMPLETED requires debit SUCCEEDED, CUT credit SUCCEEDED, exact Money and a principal BankPostingReference. | `DOMAIN` | `REJECT_INVALID_SNAPSHOT` | `DOMAIN_UNIT` |
| `PAY-INV-045` | DEBIT_CONFIRMED_CUT_CREDIT_PENDING requires debit SUCCEEDED, CUT credit PENDING or UNKNOWN, a principal posting reference and no success finalization. | `DOMAIN` | `REJECT_INVALID_SNAPSHOT` | `DOMAIN_UNIT` |
| `PAY-INV-046` | A confirmed debit with explicit CUT-credit failure requires reversal review/authorization and can never be represented as successful or failed-without-effect. | `DOMAIN` | `REVERSAL_REQUIRED` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-047` | When principal or leg references are present in multiple domain objects, they must be equal and immutable. | `DOMAIN` | `INTEGRATION_CONFLICT` | `DOMAIN_UNIT` |
| `PAY-INV-048` | The original BankPostingReference is retained for the lifetime of Payment, including after TFJ processing or reversal. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `RECONSTITUTION_TEST` |
| `PAY-INV-049` | Confirmed debit and CUT credit establish immediate posting facts only; they never establish TFJ finality. | `DOMAIN` | `REJECT_INVALID_TRANSITION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-050` | A posting or posting-resolution mutation is committed atomically with aggregate state, immutable audit and Outbox intent, or none of them is committed. | `APPLICATION`, `PERSISTENCE` | `ROLLBACK_ALL` | `POSTGRES_INTEGRATION`, `CRASH_RECOVERY_TEST` |

## Notification and TFJ finality
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-051` | Notification is a domain intent/event orthogonal to financial state; delivery success or failure never changes Payment financial status. | `DOMAIN`, `APPLICATION` | `IGNORE_DELIVERY_FOR_FINANCIAL_STATE` | `DOMAIN_UNIT`, `INTERMODULE_TEST` |
| `PAY-INV-052` | At most one logical notification intent exists per Payment, notification purpose and business result version; replay reuses the original event identity. | `APPLICATION`, `PERSISTENCE` | `RETURN_EXISTING_INTENT` | `POSTGRES_INTEGRATION`, `REPLAY_TEST` |
| `PAY-INV-053` | TFJ tracking begins from a confirmed completed posting and does not wait for notification delivery acknowledgement. | `DOMAIN`, `APPLICATION` | `REJECT_INVALID_DEPENDENCY` | `DOMAIN_UNIT`, `INTERMODULE_TEST` |
| `PAY-INV-054` | An EndOfDayConfirmationSnapshot enters Payment only after authentication, durable persistence and unique matching on institution, business date, public Payment reference and principal posting reference. | `ACCOUNTING`, `APPLICATION`, `DOMAIN` | `QUARANTINE_NO_PAYMENT_MUTATION` | `APPLICATION`, `POSTGRES_INTEGRATION`, `CONTRACT_TEST` |
| `PAY-INV-055` | TFJ PENDING, unmatched, ambiguous or conflicting evidence remains outside Payment and cannot change its state. | `ACCOUNTING`, `DOMAIN` | `QUARANTINE_NO_PAYMENT_MUTATION` | `APPLICATION`, `RECONCILIATION_TEST` |
| `PAY-INV-056` | Only a uniquely matched TFJ status INTEGRATED establishes TREASURY_INTEGRATED and finalization. | `DOMAIN` | `REJECT_INVALID_TRANSITION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-057` | TFJ delay or cutoff expiry keeps Payment pending and cannot alone authorize reversal, failure or another posting. | `DOMAIN`, `APPLICATION` | `REMAIN_PENDING_REQUEST_LOOKUP` | `DOMAIN_UNIT`, `SCHEDULER_TEST` |
| `PAY-INV-058` | An identical terminal TFJ replay is a no-op; conflicting final TFJ evidence is quarantined and never silently replaces terminal evidence. | `DOMAIN`, `ACCOUNTING` | `NO_OP_OR_QUARANTINE` | `DOMAIN_UNIT`, `REPLAY_TEST`, `RECONCILIATION_TEST` |

## Reversal, failures and terminal states
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-059` | A reversal can be requested only for a confirmed or authoritatively reconciled financial effect and requires BANK_INSTRUCTION or APPROVED_RUNBOOK authorization. | `DOMAIN`, `APPLICATION`, `POLICY` | `REJECT_REVERSAL` | `DOMAIN_UNIT`, `POLICY_TEST`, `AUTHORIZATION_TEST` |
| `PAY-INV-060` | Reversal uses a distinct immutable idempotency key and reversal reference and never overwrites the original posting identity. | `DOMAIN`, `APPLICATION` | `IDEMPOTENCY_OR_REFERENCE_CONFLICT` | `DOMAIN_UNIT`, `APPLICATION` |
| `PAY-INV-061` | Reversal outcome UNKNOWN requires authoritative lookup and forbids blind reversal resubmission. | `DOMAIN`, `APPLICATION` | `ACCOUNTING_OUTCOME_UNKNOWN` | `DOMAIN_UNIT`, `RESILIENCE_TEST` |
| `PAY-INV-062` | REVERSED requires an authoritative outcome REVERSED with a stable reversal reference linked to the same Payment and original posting. | `DOMAIN` | `REJECT_INVALID_TRANSITION` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-063` | A rejected or not-allowed reversal does not make the original financial effect disappear and cannot transition Payment to FAILED-without-effect. | `DOMAIN` | `REMAIN_REVERSAL_REQUIRED_OR_OPERATIONAL` | `DOMAIN_UNIT` |
| `PAY-INV-064` | REJECTED is terminal only for a conclusive business/security rejection before any financial instruction can have produced an effect. | `DOMAIN` | `REJECT_INVALID_TERMINAL_STATE` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-065` | FAILED is terminal only when absence of financial effect is proven; an uncertain, partial or reversal-required Payment cannot become FAILED. | `DOMAIN` | `REJECT_INVALID_TERMINAL_STATE` | `DOMAIN_UNIT`, `STATE_TRANSITION_TEST` |
| `PAY-INV-066` | Every terminal state has finalizedAt, immutable terminal evidence and no later state-changing command; an identical fact replay remains a no-op. | `DOMAIN` | `REJECT_NO_MUTATION` | `DOMAIN_UNIT`, `REPLAY_TEST` |

## Replay, concurrency, audit and atomicity
| ID | Invariant | Enforcement | Violation | Verification |
| --- | --- | --- | --- | --- |
| `PAY-INV-067` | Same PaymentSource and ExternalPaymentReference with the same canonical request fingerprint resolves to the original Payment and creates no new business effect. | `APPLICATION`, `PERSISTENCE` | `RETURN_EXISTING_PAYMENT` | `APPLICATION`, `CONCURRENCY` |
| `PAY-INV-068` | Same source/reference or idempotency scope with a different canonical request fingerprint is a conflict and leaves the original Payment unchanged. | `APPLICATION`, `PERSISTENCE` | `IDEMPOTENCY_CONFLICT` | `APPLICATION`, `CONCURRENCY` |
| `PAY-INV-069` | Same evidence identity and fingerprint is a no-op without version increment or new domain event; same identity with another fingerprint is a conflict. | `DOMAIN` | `NO_OP_OR_CONFLICT` | `DOMAIN_UNIT`, `REPLAY_TEST` |
| `PAY-INV-070` | A command with an expected business version different from the current version is rejected before mutation. | `DOMAIN`, `PERSISTENCE` | `STALE_COMMAND` | `DOMAIN_UNIT`, `CONCURRENCY` |
| `PAY-INV-071` | Concurrent creation for the same business uniqueness key results in one Payment; losing transactions reload the winner or return a conflict. | `APPLICATION`, `PERSISTENCE` | `ONE_WINNER` | `POSTGRES_INTEGRATION`, `CONCURRENCY` |
| `PAY-INV-072` | Aggregate reconstitution restores structurally valid persisted state, performs no business transition, changes no timestamp/version and raises no event. | `DOMAIN`, `PERSISTENCE` | `REJECT_CORRUPT_STATE` | `RECONSTITUTION_TEST`, `PERSISTENCE_TEST` |
| `PAY-INV-073` | An invariant violation causes no partial state mutation, no event registration, no audit transition, no Outbox intent and no version change. | `DOMAIN`, `APPLICATION`, `PERSISTENCE` | `ATOMIC_NO_OP` | `DOMAIN_UNIT`, `POSTGRES_INTEGRATION` |
| `PAY-INV-074` | Every successful aggregate mutation registers the exact corresponding domain fact once and commits state, audit and Outbox intent atomically. | `DOMAIN`, `APPLICATION`, `PERSISTENCE` | `ROLLBACK_ALL` | `DOMAIN_UNIT`, `POSTGRES_INTEGRATION` |
| `PAY-INV-075` | Outbox republication and consumer redelivery preserve eventId and payload identity and never repeat the aggregate transition or financial effect. | `APPLICATION`, `INTEGRATION` | `IDEMPOTENT_REDELIVERY` | `MESSAGING_INTEGRATION`, `REPLAY_TEST` |
| `PAY-INV-076` | The aggregate never performs a network call; external results are accepted only after application orchestration returns a canonical result. | `DOMAIN`, `ARCHITECTURE` | `ARCHITECTURE_FAILURE` | `ARCHITECTURE_TEST`, `APPLICATION` |


## Policy and Domain Service bindings added by Lot 2.7

The machine-readable invariant catalogue now associates every applicable
`PAY-INV-*` rule with:

```text
policyRefs
domainServiceRefs
```

Examples:

| Invariant family | Decision components |
| --- | --- |
| Authorization and freshness | `PAY-POL-001`, `PAY-POL-002`, `PAY-POL-006` |
| Banking and funds | `PAY-POL-003`, `PAY-POL-004` |
| Treasury resolution | `PAY-POL-005` |
| Posting authorization | `PAY-POL-007` |
| Posting outcomes | `PAY-POL-006`, `PAY-POL-008`, `PAY-POL-012`, `PAY-DS-001` |
| TFJ finality | `PAY-POL-006`, `PAY-POL-009`, `PAY-POL-012`, `PAY-DS-002` |
| Reversal | `PAY-POL-006`, `PAY-POL-010`, `PAY-POL-011`, `PAY-POL-012`, `PAY-DS-003` |
| Result intent | `PAY-POL-013`, `PAY-DS-004` |
| Event confidentiality | `PAY-POL-014` |

Policies and services do not replace invariant enforcement by the aggregate;
they provide reusable pure decisions consumed by named operations.

## 13. State and evidence coherence

| Payment condition | Required evidence | Forbidden evidence or effect |
| --- | --- | --- |
| Newly received | Complete immutable request identity and intent | Authorization, bank, posting, TFJ or reversal success assumed |
| Authorization approved | Approved authorization evidence | Any mismatched mandatory binding |
| Banking verified | Authorization approved + banking outcome `VERIFIED` | ObservedCustomer as authorization source |
| Financially approved | Banking `VERIFIED` + funds `VERIFIED` + Treasury `RESOLVED` | Stale funds result or unresolved CUT |
| Posting submitted | One durable logical instruction and stable idempotency identity | Second logical posting |
| Debit-only known | Debit leg `SUCCEEDED`, CUT leg not `SUCCEEDED`, principal posting reference | Success/finality |
| CUT credited | Debit and CUT legs `SUCCEEDED`, principal posting reference | TFJ finality |
| Accounting outcome unknown | Canonical `UNKNOWN` or incomplete evidence | Failure assumption or blind replay |
| Pending TFJ | Completed posting, no accepted final TFJ | Dependence on notification delivery |
| Treasury integrated | Uniquely matched TFJ `INTEGRATED` snapshot | Later state-changing mutation |
| Reversal required | Confirmed/reconciled financial effect plus reason | Automatic reversal |
| Reversal pending | Approved authorization and durable reversal instruction | Reuse of original posting key |
| Reversed | Authoritative `REVERSED` outcome and reversal reference | Deletion of original posting evidence |
| Rejected | Conclusive pre-financial business/security reason | Any possible financial effect |
| Failed | Proven absence of financial effect | Unknown, partial or unreversed effect |

## 14. Temporal invariants

1. `createdAt <= updatedAt`.
2. Snapshot `observedAt <= acceptedAt`.
3. Favorable funds evidence must be accepted no later than `validUntil`.
4. `finalizedAt` is required only for terminal states and is not before
   `createdAt` or the terminal evidence time.
5. A posting leg `effectiveAt`, TFJ `confirmedAt` and reversal result time cannot
   be later than the snapshot `acceptedAt`.
6. Business dates are bank-local `LocalDate`; technical instants remain UTC.
7. Clock-skew and freshness windows are policies defined in Lot 2.7, not
   hard-coded domain constants.

**Decision:** `PAY-DEC-IA1-028`.

## 15. Status-classification decisions

- `NOTIFIED` is not a financial lifecycle state in the IA-1 target model. A
  notification intent is an event/Outbox fact associated with a result version.
- Banking verification and funds control remain distinct gates even if an
  interim orchestration uses one broad processing status.
- `FAILED` means processing terminated with proven absence of financial effect.
- `ACCOUNTING_OUTCOME_UNKNOWN` is non-terminal.
- `TREASURY_INTEGRATED` and `REVERSED` are terminal financial outcomes.
- A provider TFJ status `FAILED` does not automatically map to Payment
  `FAILED`; it may require reconciliation or reversal.

**Decisions:** `PAY-DEC-IA1-029`, `PAY-DEC-IA1-030`.

## 16. One-posting rule

One logical Payment intention can produce only one logical posting instruction.

Permitted repetitions:

- resending the exact same instruction with the exact same bank idempotency key
  when the approved recovery policy permits it;
- authoritative lookup by the original idempotency key;
- authoritative lookup by the principal bank posting reference.

Forbidden repetitions:

- generating a new posting key for the same Payment;
- changing amount, debtor account, Treasury account or allocation;
- retrying blindly after an uncertain outcome;
- creating a replacement Payment to bypass the invariant.

**Decision:** `PAY-DEC-IA1-031`.

## 17. Machine-readable companion

The same catalogue is available in:

`documentation/ai/payment/PAYMENT_INVARIANT_CATALOGUE.yaml`

The Markdown and YAML files must contain the same 76 invariant identifiers.

## 18. Verification requirements

Every invariant must have at least one executable verification before code
generation is authorized.

Minimum mapping:

| Enforcement | Required verification |
| --- | --- |
| Value Object / Domain | Pure Java unit or property-based test |
| Application | Use-case test with controlled ports |
| Persistence | PostgreSQL Testcontainers integration test |
| Integration / Contract | Mapping and OpenAPI/consumer contract test |
| Architecture | ArchUnit or equivalent module-dependency test |
| Concurrency | Parallel transaction test against PostgreSQL |
| Financial recovery | Timeout/lookup/reconciliation resilience test |
| Security | Serialization, masking and sensitive-data absence test |

`PAYMENT_ACCEPTANCE_SCENARIOS.md` remains the IA-0P acceptance baseline. Its
names and scenario references will be fully normalized during Lot 2.8.

## 19. Decisions closed in Lot 2.4

| Decision | Result |
| --- | --- |
| `PAY-DEC-IA1-027` | IA-1 invariant semantics override conflicting IA-0P state-machine semantics. |
| `PAY-DEC-IA1-028` | Temporal invariants use UTC instants, bank-local business dates and policy-driven freshness. |
| `PAY-DEC-IA1-029` | Notification intent is orthogonal and `NOTIFIED` is not a target financial state. |
| `PAY-DEC-IA1-030` | TFJ failure does not map automatically to Payment `FAILED`. |
| `PAY-DEC-IA1-031` | One logical posting instruction exists per Payment; recovery reuses its identity. |
| `PAY-DEC-IA1-032` | Invariant violation is atomic and produces no mutation/event/version change. |

## 20. Exit checklist

- [ ] All identity and immutable-intent rules are explicit.
- [ ] Every external evidence category has binding and freshness invariants.
- [ ] Banking verification and funds control are distinct.
- [ ] Posting success, partial effect, unknown outcome and no-effect rejection
      are mutually exclusive.
- [ ] Notification delivery is independent from financial state.
- [ ] TFJ matching and finality are explicit.
- [ ] Reversal preserves original identities.
- [ ] Terminal-state classification is unambiguous.
- [ ] Replay and concurrency produce no second effect.
- [ ] Every invariant identifies enforcement and verification.
- [ ] Markdown and YAML invariant IDs match.
- [ ] Code generation remains forbidden.

## 21. Verdict

```text
IA-1 LOT 2.4 PAYMENT INVARIANTS PREPARED
INVARIANT COUNT: 76
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.8 — FINAL MODEL VALIDATION
CODE GENERATION: FORBIDDEN
```
