# SIXPAY CONNECT — Payment Acceptance Scenarios

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `2.8 — Final Model Validation`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `FINAL_VALIDATED_ACCEPTANCE_BASELINE`  
> **Code generation:** **FORBIDDEN_PENDING_EXPLICIT_APPROVAL**

## 1. Purpose

This file supersedes the IA-0P acceptance catalogue and is aligned with the
final IA-1 Payment model.

The authoritative model contains:

```text
17 states
38 legal transitions
76 invariants
16 application commands
17 aggregate operations
33 Payment domain events
14 policies
4 pure Domain Services
```

## 2. Test principles

- Domain, policy and Domain Service tests run without Spring.
- Time, UUIDs, profile versions and external outcomes are controlled.
- PostgreSQL integration tests use Testcontainers rather than H2.
- Every negative financial test proves the exact number of external writes.
- No replay or retry creates a replacement posting or reversal identity.
- Event tests validate schema, ordering, confidentiality and deduplication.
- Raw external payloads, credentials and clear accounts never enter fixtures
  intended for domain or event serialization.
- A critical uncovered invariant, transition, event or policy blocks the gate.

## 3. Generated exhaustive test sets

The implementation must generate these parameterized cases directly from the
machine-readable catalogues:

| Generated set | Required cases |
| --- | ---: |
| Invariant verification | 76 |
| Legal state transitions | 38 |
| Illegal state/command combinations | Exhaustive complement |
| Event fact-kind/schema mapping | 33 |
| Policy decision tables | 14 |
| Domain Service decision tables | 4 |
| Terminal-state mutation attempts | Every command × 4 terminal states |
| Event sensitive-field denylist | Every event × denylist category |

The parameterized tests preserve the normative identifiers in test names and
reports.

## 4. Named scenario families

| Family | Level | Named scenarios |
| --- | --- | ---: |
| `PAY-ACC-MDL` — Aggregate and invariant model | `DOMAIN` | 12 |
| `PAY-ACC-VO` — Value Objects | `DOMAIN` | 10 |
| `PAY-ACC-SNP` — Snapshots and evidence | `DOMAIN` | 10 |
| `PAY-ACC-STM` — State machine | `DOMAIN` | 12 |
| `PAY-ACC-EVT` — Domain Events and Outbox mapping | `DOMAIN_APPLICATION` | 12 |
| `PAY-ACC-POL` — Policies | `DOMAIN` | 14 |
| `PAY-ACC-DS` — Pure Domain Services | `DOMAIN` | 4 |
| `PAY-ACC-IDM` — Idempotence and replay | `APPLICATION_PERSISTENCE` | 10 |
| `PAY-ACC-CON` — Concurrency | `APPLICATION_PERSISTENCE` | 8 |
| `PAY-ACC-AMP` — Amplitude integration | `INTEGRATION` | 14 |
| `PAY-ACC-NOT` — Notification | `INTERMODULE` | 10 |
| `PAY-ACC-TFJ` — TFJ finality and reconciliation | `ACCOUNTING_INTERMODULE` | 12 |
| `PAY-ACC-SEC` — Security and confidentiality | `SECURITY_INTEGRATION` | 15 |
| `PAY-ACC-OBS` — ObservedCustomer projection | `REPORTING_CUSTOMER` | 10 |
| `PAY-ACC-PAR` — Partial and uncertain financial outcomes | `DOMAIN_INTEGRATION` | 9 |
| `PAY-ACC-REV` — Reversal | `DOMAIN_ACCOUNTING` | 12 |

**Total named scenarios:** `174`.

## MDL — Aggregate and invariant model

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-MDL-001` | Create one valid Payment intent. | RECEIVED and PaymentReceived; immutable identities. | `PAY-INV-001..020` |
| `PAY-ACC-MDL-002` | Reject zero, negative or currency-invalid Money. | No Payment, version or event. | `PAY-INV-008, PAY-INV-020` |
| `PAY-ACC-MDL-003` | Reject unbalanced Treasury allocations. | Creation rejected with no mutation. | `PAY-INV-008` |
| `PAY-ACC-MDL-004` | Protect every immutable original-intent field. | No setter or replacement path. | `PAY-INV-001..009` |
| `PAY-ACC-MDL-005` | Reject clear account, credential or raw payload material. | Security rejection before persistence/event. | `PAY-INV-011..018` |
| `PAY-ACC-MDL-006` | Increment businessVersion exactly once per real mutation. | No increment for replay or rejection. | `PAY-INV-010` |
| `PAY-ACC-MDL-007` | Reconstitute a valid aggregate. | Same state/version, no event. | `PAY-OP-017` |
| `PAY-ACC-MDL-008` | Reject incoherent reconstitution. | Stable domain exception. | `PAY-INV-015` |
| `PAY-ACC-MDL-009` | Keep terminal state immutable. | No mutation/event/version increment. | `PAY-INV-070` |
| `PAY-ACC-MDL-010` | Preserve original posting evidence after reversal. | REVERSED with posting evidence retained. | `PAY-INV-062..064` |
| `PAY-ACC-MDL-011` | Keep ObservedCustomer non-authoritative. | No Payment approval from projection. | `PAY-INV-031` |
| `PAY-ACC-MDL-012` | Keep Notification delivery outside Payment. | Delivery outcome never changes state. | `PAY-INV-051..052` |

## VO — Value Objects

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-VO-001` | PaymentId accepts one non-nil UUID. | Opaque immutable identity. | `PAY-VO-PaymentId` |
| `PAY-ACC-VO-002` | ExternalPaymentReference normalizes strictly. | Stable source-scoped equality. | `PAY-INV-002..003` |
| `PAY-ACC-VO-003` | ExternalSubscriptionReference remains external-only. | No local subscription behavior. | `PAY-INV-005, PAY-INV-026` |
| `PAY-ACC-VO-004` | PublicPaymentReference is distinct and globally unique. | No equality with external/bank references. | `PAY-INV-004` |
| `PAY-ACC-VO-005` | DebtorAccountReference separates token, mask and fingerprint. | No clear account exposure. | `PAY-INV-012` |
| `PAY-ACC-VO-006` | TreasuryAccountReference requires protected configuration origin. | Inbound account cannot create/replace it. | `PAY-INV-035..038` |
| `PAY-ACC-VO-007` | Money and allocations share one currency. | Exact positive sum. | `PAY-INV-008` |
| `PAY-ACC-VO-008` | PostingInstructionIdentity is immutable. | Same identity reused for retry/lookup. | `PAY-INV-039..042` |
| `PAY-ACC-VO-009` | ReversalInstructionIdentity differs from posting identity. | No key reuse. | `PAY-INV-059..061` |
| `PAY-ACC-VO-010` | PaymentFailure contains stable safe fields. | No free-form provider diagnostics. | `PAY-INV-018, PAY-INV-065..068` |

## SNP — Snapshots and evidence

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-SNP-001` | Accept structurally valid authorization evidence. | Bounded immutable snapshot. | `PAY-POL-001..002` |
| `PAY-ACC-SNP-002` | Reject stale or future-dated authorization evidence. | No banking progression. | `PAY-INV-022` |
| `PAY-ACC-SNP-003` | Accept verified banking evidence bound to bank/account. | Progress to funds control. | `PAY-INV-027..030` |
| `PAY-ACC-SNP-004` | Treat banking INDETERMINATE as processing only. | No posting approval. | `PAY-INV-029` |
| `PAY-ACC-SNP-005` | Accept funds evidence for exact amount/account/currency. | Progress only while fresh. | `PAY-INV-032..034` |
| `PAY-ACC-SNP-006` | Reject inferred or extended funds validity. | No posting authorization. | `PAY-POL-004` |
| `PAY-ACC-SNP-007` | Accept protected Treasury resolution. | Configuration identity/version retained. | `PAY-INV-035..038` |
| `PAY-ACC-SNP-008` | Classify identical evidence replay as no-op. | No version or event. | `PAY-POL-006` |
| `PAY-ACC-SNP-009` | Reject same evidence identity with another fingerprint. | Conflict/quarantine. | `PAY-POL-006` |
| `PAY-ACC-SNP-010` | Retain one current snapshot per category. | History remains audit/reporting-owned. | `PAY-INV-015` |

## STM — State machine

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-STM-001` | Execute the complete nominal path. | TREASURY_INTEGRATED. | `PAY-TR-001..030` |
| `PAY-ACC-STM-002` | Reject authorization conclusively. | REJECTED without bank call. | `PAY-TR-003` |
| `PAY-ACC-STM-003` | Handle banking rejection. | REJECTED and immediate result. | `PAY-TR-005` |
| `PAY-ACC-STM-004` | Handle banking indeterminate. | Same processing stage and PROCESSING result. | `PAY-TR-006` |
| `PAY-ACC-STM-005` | Handle funds rejection or indeterminate. | REJECTED or controlled processing. | `PAY-TR-008, PAY-TR-010` |
| `PAY-ACC-STM-006` | Authorize exactly one posting. | POSTING_PENDING. | `PAY-TR-013` |
| `PAY-ACC-STM-007` | Separate posting UNKNOWN. | POSTING_OUTCOME_UNKNOWN and lookup only. | `PAY-TR-017..023` |
| `PAY-ACC-STM-008` | Represent debit-only outcome. | DEBIT_CONFIRMED or REVERSAL_REQUIRED. | `PAY-TR-014..015, PAY-TR-023` |
| `PAY-ACC-STM-009` | Accept uniquely matched TFJ finality. | TREASURY_INTEGRATED. | `PAY-TR-027` |
| `PAY-ACC-STM-010` | Require explicit reversal authorization. | REVERSAL_PENDING. | `PAY-TR-029` |
| `PAY-ACC-STM-011` | Separate reversal UNKNOWN. | REVERSAL_OUTCOME_UNKNOWN and lookup only. | `PAY-TR-032..035` |
| `PAY-ACC-STM-012` | Reject every undeclared source/command/outcome combination. | No mutation/event/version. | `All forbidden transitions` |

## EVT — Domain Events and Outbox mapping

**Level:** `DOMAIN_APPLICATION`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-EVT-001` | Register PaymentReceived after creation. | One event with resulting version. | `PAY-EVT-001` |
| `PAY-ACC-EVT-002` | Register ordered multiple events for one mutation. | Same aggregateVersion; eventSequence 1..n. | `PAY-DEC-IA1-044` |
| `PAY-ACC-EVT-003` | Map every fact kind to one event type. | 33/33 one-to-one mapping. | `PAY-EVT-001..033` |
| `PAY-ACC-EVT-004` | Persist state, audit and Outbox atomically. | All or nothing. | `PAY-INV-073..076` |
| `PAY-ACC-EVT-005` | Republish with identical eventId/schema/payload. | No new business fact. | `PAY-EVT-RULE-005` |
| `PAY-ACC-EVT-006` | Deduplicate consumer processing by eventId. | One consumer effect. | `PAY-EVT-RULE-009` |
| `PAY-ACC-EVT-007` | Reject same eventId with different payload. | Quarantine and alert. | `PAY-EVT-RULE-005` |
| `PAY-ACC-EVT-008` | Reject unsupported event schema version. | Version quarantine, no effect. | `consumerPolicy` |
| `PAY-ACC-EVT-009` | Prevent automatic aggregate/snapshot serialization. | Explicit payload only. | `PAY-POL-014` |
| `PAY-ACC-EVT-010` | Reject protected or sensitive event fields. | Disclosure decision rejects. | `PAY-POL-014` |
| `PAY-ACC-EVT-011` | Trigger Notification only from result-intent events. | Exact three-event set. | `PAY-EVT-006,024,029` |
| `PAY-ACC-EVT-012` | Keep PaymentNotificationDelivered outside Payment. | No Payment mutation. | `PAY-DEC-IA1-047` |

## POL — Policies

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-POL-001` | EvidenceTemporalValidityPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-001` |
| `PAY-ACC-POL-002` | AuthorizationEvidenceAcceptancePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-002` |
| `PAY-ACC-POL-003` | BankingVerificationAcceptancePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-003` |
| `PAY-ACC-POL-004` | FundsControlAcceptancePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-004` |
| `PAY-ACC-POL-005` | TreasuryResolutionAcceptancePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-005` |
| `PAY-ACC-POL-006` | EvidenceReplayReplacementPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-006` |
| `PAY-ACC-POL-007` | PostingInstructionAuthorizationPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-007` |
| `PAY-ACC-POL-008` | PostingOutcomeInterpretationPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-008` |
| `PAY-ACC-POL-009` | EndOfDayConfirmationAcceptancePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-009` |
| `PAY-ACC-POL-010` | ReversalAuthorizationPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-010` |
| `PAY-ACC-POL-011` | ReversalOutcomeInterpretationPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-011` |
| `PAY-ACC-POL-012` | FailureClassificationPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-012` |
| `PAY-ACC-POL-013` | PaymentResultIntentPolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-013` |
| `PAY-ACC-POL-014` | PaymentEventDisclosurePolicy complete decision table. | Every declared output is reachable only under its documented conditions; purity preserved. | `PAY-POL-014` |

## DS — Pure Domain Services

**Level:** `DOMAIN`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-DS-001` | PostingOutcomeDecisionService coordinates its policies. | Returns PostingDecision without I/O, mutation or event registration. | `PAY-DS-001` |
| `PAY-ACC-DS-002` | EndOfDayDecisionService coordinates its policies. | Returns EndOfDayDecision without I/O, mutation or event registration. | `PAY-DS-002` |
| `PAY-ACC-DS-003` | ReversalDecisionService coordinates its policies. | Returns ReversalDecision without I/O, mutation or event registration. | `PAY-DS-003` |
| `PAY-ACC-DS-004` | PaymentResultIntentService coordinates its policies. | Returns ResultIntentDecision without I/O, mutation or event registration. | `PAY-DS-004` |

## IDM — Idempotence and replay

**Level:** `APPLICATION_PERSISTENCE`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-IDM-001` | First inbound request. | One registry record and one Payment. | `PAY-INV-003, PAY-CMD-001` |
| `PAY-ACC-IDM-002` | Identical request while processing. | Same Payment and current PROCESSING result. | `PAY-INV-069` |
| `PAY-ACC-IDM-003` | Identical completed request. | Same result, no new effect. | `PAY-INV-069` |
| `PAY-ACC-IDM-004` | Same key with different fingerprint. | Conflict; original intact. | `PAY-INV-069` |
| `PAY-ACC-IDM-005` | External reference reused inconsistently. | Conflict and audit. | `PAY-INV-003` |
| `PAY-ACC-IDM-006` | Posting request redelivered. | Same posting identity; no second instruction. | `PAY-INV-039..042` |
| `PAY-ACC-IDM-007` | Posting UNKNOWN replay attempted. | Read-only lookup; no write replay. | `PAY-INV-042` |
| `PAY-ACC-IDM-008` | Reversal request redelivered. | Same reversal identity. | `PAY-INV-059..061` |
| `PAY-ACC-IDM-009` | Notification event redelivered. | One logical delivery per source event/phase. | `PAY-EVT-006,024,029` |
| `PAY-ACC-IDM-010` | Projection rebuilt from immutable events. | Same functional projection. | `PAY-EVT-RULE-010` |

## CON — Concurrency

**Level:** `APPLICATION_PERSISTENCE`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-CON-001` | Two identical creations race. | One Payment; loser reloads existing. | `PAY-INV-003` |
| `PAY-ACC-CON-002` | Two conflicting creations race. | One winner; one conflict. | `PAY-INV-003` |
| `PAY-ACC-CON-003` | Two commands use the same expected version. | One mutation; one stale rejection. | `PAY-INV-071` |
| `PAY-ACC-CON-004` | Callback and posting lookup race. | One coherent authoritative resolution. | `PAY-INV-042` |
| `PAY-ACC-CON-005` | TFJ push and fallback race. | One unique match and one Payment mutation. | `PAY-INV-054..058` |
| `PAY-ACC-CON-006` | Two reversal authorizations race. | One reversal instruction. | `PAY-INV-059` |
| `PAY-ACC-CON-007` | Two Outbox workers claim one item. | One active claim. | `PAY-INV-075` |
| `PAY-ACC-CON-008` | Two consumers receive one event. | One committed consumer effect. | `PAY-EVT-RULE-009` |

## AMP — Amplitude integration

**Level:** `INTEGRATION`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-AMP-001` | Verified client/account. | Canonical verified banking snapshot. | `PAY-POL-003` |
| `PAY-ACC-AMP-002` | Client absent. | Stable business rejection. | `PAY-POL-003` |
| `PAY-ACC-AMP-003` | Account ownership mismatch. | No funds/posting request. | `PAY-INV-030` |
| `PAY-ACC-AMP-004` | Restriction/opposition. | Stable rejection. | `PAY-POL-003` |
| `PAY-ACC-AMP-005` | Funds sufficient. | Verified funds snapshot with validUntil. | `PAY-POL-004` |
| `PAY-ACC-AMP-006` | Funds insufficient. | Business rejection, no posting. | `PAY-INV-033` |
| `PAY-ACC-AMP-007` | Read timeout. | Bounded retry outside domain. | `PAYMENT_RESILIENCE_BASELINE` |
| `PAY-ACC-AMP-008` | Permanent schema/business error. | No automatic retry. | `PAYMENT_RESILIENCE_BASELINE` |
| `PAY-ACC-AMP-009` | Posting complete. | POSTED_PENDING_TFJ. | `PAY-DS-001` |
| `PAY-ACC-AMP-010` | Posting timeout after possible submission. | POSTING_OUTCOME_UNKNOWN. | `PAY-DS-001` |
| `PAY-ACC-AMP-011` | Posting lookup finds completed instruction. | Resolution without second posting. | `PAY-DS-001` |
| `PAY-ACC-AMP-012` | Posting lookup proves no financial effect. | REJECTED/FAILED according to failure category. | `PAY-POL-012` |
| `PAY-ACC-AMP-013` | Malformed provider payload. | Mapping rejection before domain. | `PAY-INV-013` |
| `PAY-ACC-AMP-014` | Bank authentication failure. | Closed failure; no secret exposure. | `PAY-INV-011, PAY-INV-018` |

## NOT — Notification

**Level:** `INTERMODULE`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-NOT-001` | Immediate rejected result. | One immediate delivery intent. | `PAY-EVT-006` |
| `PAY-ACC-NOT-002` | Immediate failed result. | One immediate delivery intent. | `PAY-EVT-006` |
| `PAY-ACC-NOT-003` | Immediate processing result. | PROCESSING without finality. | `PAY-EVT-006` |
| `PAY-ACC-NOT-004` | Posting pending TFJ result. | Immediate POSTED_PENDING_TFJ. | `PAY-EVT-006` |
| `PAY-ACC-NOT-005` | Final Treasury result. | Only after matched TFJ INTEGRATED. | `PAY-EVT-024` |
| `PAY-ACC-NOT-006` | Reversal success result. | REVERSAL_REVERSED. | `PAY-EVT-029` |
| `PAY-ACC-NOT-007` | Reversal rejected/not allowed. | REVERSAL_REQUIRED result. | `PAY-EVT-029` |
| `PAY-ACC-NOT-008` | Transient delivery failure. | Retry same source event/phase. | `PAY-EVT-RULE-009` |
| `PAY-ACC-NOT-009` | Permanent delivery failure. | DLQ/alert; Payment unchanged. | `PAY-INV-052` |
| `PAY-ACC-NOT-010` | Delivery event replay. | No Payment transition. | `PAY-DEC-IA1-047` |

## TFJ — TFJ finality and reconciliation

**Level:** `ACCOUNTING_INTERMODULE`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-TFJ-001` | Unique matching INTEGRATED confirmation. | TREASURY_INTEGRATED. | `PAY-DS-002` |
| `PAY-ACC-TFJ-002` | Identical duplicate confirmation. | No-op. | `PAY-POL-006` |
| `PAY-ACC-TFJ-003` | Conflicting duplicate confirmation. | Quarantine conflict. | `PAY-POL-006` |
| `PAY-ACC-TFJ-004` | No matching Payment. | Accounting quarantine; Payment untouched. | `PAY-INV-054` |
| `PAY-ACC-TFJ-005` | Multiple matching candidates. | Quarantine; no domain command. | `PAY-INV-054` |
| `PAY-ACC-TFJ-006` | Institution/date/reference mismatch. | Reject match proof. | `PAY-POL-009` |
| `PAY-ACC-TFJ-007` | PENDING provider status. | Not accepted as final confirmation. | `PAY-POL-009` |
| `PAY-ACC-TFJ-008` | Matched adverse result requiring reversal. | REVERSAL_REQUIRED. | `PAY-DS-002` |
| `PAY-ACC-TFJ-009` | Matched adverse result requiring manual reconciliation. | Payment remains non-final. | `PAY-DS-002` |
| `PAY-ACC-TFJ-010` | Fallback finds same confirmation. | One canonical command. | `PAY-INV-058` |
| `PAY-ACC-TFJ-011` | TFJ absent at operational cutoff. | Operational alert; no automatic reversal. | `PAY-INV-057` |
| `PAY-ACC-TFJ-012` | Final result notification. | One final result intent. | `PAY-EVT-024` |

## SEC — Security and confidentiality

**Level:** `SECURITY_INTEGRATION`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-SEC-001` | Missing or invalid authorization token. | Reject before Payment creation. | `PAY-INV-019` |
| `PAY-ACC-SEC-002` | Missing or invalid Subscription Key. | Reject before Payment creation. | `PAY-INV-019` |
| `PAY-ACC-SEC-003` | Only one credential is valid. | Reject closed. | `PAY-INV-019` |
| `PAY-ACC-SEC-004` | JWT signature/JWKS verification fails. | No fabricated evidence. | `PAY-INV-024` |
| `PAY-ACC-SEC-005` | Authorization bindings mismatch. | Security rejection. | `PAY-POL-002` |
| `PAY-ACC-SEC-006` | Raw JWT reaches domain mapper. | Mapping/security failure. | `PAY-INV-011` |
| `PAY-ACC-SEC-007` | Clear debtor account appears in event. | Disclosure rejection. | `PAY-POL-014` |
| `PAY-ACC-SEC-008` | Clear Treasury account appears in event. | Disclosure rejection. | `PAY-POL-014` |
| `PAY-ACC-SEC-009` | Raw KYC/customer identity appears in event. | Disclosure rejection. | `PAY-POL-014` |
| `PAY-ACC-SEC-010` | Provider error or stack trace reaches safeMessage. | Security rejection. | `PAY-INV-018` |
| `PAY-ACC-SEC-011` | Unauthorized internal query. | 403 without existence disclosure. | `PAYMENT_SECURITY_AUDIT_BASELINE` |
| `PAY-ACC-SEC-012` | Masked query response. | At most approved display representation. | `PAY-INV-012` |
| `PAY-ACC-SEC-013` | Required secret missing. | Startup/call fails closed. | `PAYMENT_SECURITY_AUDIT_BASELINE` |
| `PAY-ACC-SEC-014` | Secret/key rotation. | Controlled overlap then revocation. | `PAYMENT_SECURITY_AUDIT_BASELINE` |
| `PAY-ACC-SEC-015` | Mandatory audit unavailable. | No silent success. | `PAY-INV-074` |

## OBS — ObservedCustomer projection

**Level:** `REPORTING_CUSTOMER`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-OBS-001` | Consume PaymentReceived. | Projection created from minimized facts. | `PAY-EVT-001` |
| `PAY-ACC-OBS-002` | Consume PaymentRejected. | Rejection counters updated. | `PAY-EVT-005` |
| `PAY-ACC-OBS-003` | Consume BankingVerificationRecorded. | Minimized observed facts updated. | `PAY-EVT-007` |
| `PAY-ACC-OBS-004` | Consume TreasuryIntegrationConfirmed. | Success/finality counters updated. | `PAY-EVT-023` |
| `PAY-ACC-OBS-005` | Consume PaymentFailedWithoutFinancialEffect. | Failure counters updated. | `PAY-EVT-032` |
| `PAY-ACC-OBS-006` | Consume PaymentReversed. | Final reversal outcome updated. | `PAY-EVT-033` |
| `PAY-ACC-OBS-007` | Duplicate eventId. | Projection no-op. | `consumerPolicy` |
| `PAY-ACC-OBS-008` | Older aggregateVersion arrives later. | No projection regression. | `aggregateVersion/eventSequence` |
| `PAY-ACC-OBS-009` | Several institutions share one customer reference. | No unauthorized merge. | `PAY-INV-031` |
| `PAY-ACC-OBS-010` | Full replay rebuild. | Same functional projection. | `PAY-EVT-RULE-010` |

## PAR — Partial and uncertain financial outcomes

**Level:** `DOMAIN_INTEGRATION`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-PAR-001` | Debit and CUT credit confirmed. | POSTED_PENDING_TFJ. | `PAY-TR-013` |
| `PAY-ACC-PAR-002` | Debit confirmed, CUT pending/unknown. | DEBIT_CONFIRMED and PROCESSING. | `PAY-TR-014, PAY-TR-023` |
| `PAY-ACC-PAR-003` | Debit confirmed, CUT failed. | REVERSAL_REQUIRED. | `PAY-TR-015` |
| `PAY-ACC-PAR-004` | Posting UNKNOWN. | POSTING_OUTCOME_UNKNOWN. | `PAY-TR-017` |
| `PAY-ACC-PAR-005` | Lookup resolves complete. | POSTED_PENDING_TFJ, no second posting. | `PAY-TR-020` |
| `PAY-ACC-PAR-006` | Lookup resolves debit-only. | DEBIT_CONFIRMED. | `PAY-TR-023` |
| `PAY-ACC-PAR-007` | Lookup resolves adverse partial effect. | REVERSAL_REQUIRED. | `PAY-TR-022` |
| `PAY-ACC-PAR-008` | Lookup remains unknown. | No financial replay; processing remains controlled. | `PAY-INV-042` |
| `PAY-ACC-PAR-009` | Callback and lookup conflict. | Conflict/quarantine, no finality. | `PAY-POL-006` |

## REV — Reversal

**Level:** `DOMAIN_ACCOUNTING`

| ID | Scenario | Expected result | Traceability |
| --- | --- | --- | --- |
| `PAY-ACC-REV-001` | Authorize eligible reversal. | REVERSAL_PENDING. | `PAY-TR-029` |
| `PAY-ACC-REV-002` | Reject reversal without confirmed effect. | No mutation. | `PAY-POL-010` |
| `PAY-ACC-REV-003` | Reject unauthorized actor/runbook. | No instruction. | `PAY-POL-010` |
| `PAY-ACC-REV-004` | Reject reuse of posting idempotency key. | Conflict. | `PAY-INV-059` |
| `PAY-ACC-REV-005` | Record REVERSED. | REVERSED terminal and original evidence retained. | `PAY-TR-031` |
| `PAY-ACC-REV-006` | Record reversal UNKNOWN. | REVERSAL_OUTCOME_UNKNOWN. | `PAY-TR-032` |
| `PAY-ACC-REV-007` | Resolve UNKNOWN to REVERSED. | REVERSED. | `PAY-TR-034` |
| `PAY-ACC-REV-008` | Resolve UNKNOWN to REJECTED/NOT_ALLOWED. | REVERSAL_REQUIRED. | `PAY-TR-035` |
| `PAY-ACC-REV-009` | Replay identical reversal result. | No-op. | `PAY-POL-006` |
| `PAY-ACC-REV-010` | Conflicting reversal result. | Conflict/quarantine. | `PAY-POL-006` |
| `PAY-ACC-REV-011` | Attempt second reversal after REVERSED. | Rejected terminal mutation. | `PAY-INV-070` |
| `PAY-ACC-REV-012` | Publish reversal result intent. | Exactly one reversal-phase result. | `PAY-EVT-029` |

## Final acceptance gate

The IA-1 model validation passes only when:

- all machine-readable catalogues parse;
- all normative IDs and references are valid;
- all 38 legal transitions and the illegal complement are tested;
- all 33 events satisfy the disclosure catalogue;
- all 14 policies and 4 Domain Services satisfy purity decision tables;
- no terminal Payment can mutate;
- unknown posting and reversal outcomes remain distinct;
- notification failure never changes Payment state;
- no second financial instruction is emitted during replay, timeout or lookup;
- contract and integration prerequisites are explicitly approved.

## Verdict

```text
PAYMENT IA-1 ACCEPTANCE BASELINE: FINAL_VALIDATED
NAMED SCENARIOS: 174
GENERATED INVARIANT CASES: 76
GENERATED LEGAL TRANSITION CASES: 38
GENERATED EVENT CASES: 33
GENERATED POLICY CASES: 14
GENERATED DOMAIN SERVICE CASES: 4
CODE GENERATION: FORBIDDEN_PENDING_EXPLICIT_APPROVAL
```
