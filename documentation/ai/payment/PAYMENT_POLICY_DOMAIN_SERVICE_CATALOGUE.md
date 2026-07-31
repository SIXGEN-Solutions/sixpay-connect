# SIXPAY CONNECT — Payment Policies and Domain Services

> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `2.7 — Policies and Domain Services`  
> **Authoritative branch:** `feat/payment-domain-generation-brief`  
> **Status:** `DRAFT_PENDING_VALIDATION`  
> **Code generation:** **FORBIDDEN**

## 1. Purpose

This document defines the decision components required by the Payment model
without moving state ownership outside the `Payment` Aggregate Root.

It separates four concepts:

| Concept | Responsibility |
| --- | --- |
| Value Object / Snapshot | Validate and carry one immutable concept |
| Domain Policy | Make one pure, configurable business decision |
| Domain Service | Coordinate several policies for a cross-object interpretation |
| Port / Application / Infrastructure | Load state/configuration, call external systems, persist or publish |

## 2. Non-negotiable design rules

1. Payment remains the sole owner of state transitions.
2. Payment remains the sole registrar of Payment domain events.
3. A policy or Domain Service never mutates Payment.
4. A Domain Service returns an immutable typed decision.
5. Policies and Domain Services are stateless and deterministic.
6. They perform no repository, HTTP, Kafka, Vault, clock or configuration I/O.
7. `Instant decisionAt` and approved policy profiles are explicit inputs.
8. Spring, JPA, Jackson and integration DTOs remain outside the domain.
9. Business rejection is returned as a decision, not thrown as an exceptional
   technical condition.
10. Invalid programmer input or structurally impossible state may raise a
    stable domain exception before mutation.

## 3. Decision flow

```text
Application handler
├── loads Payment
├── resolves effective policy profiles
├── supplies explicit decisionAt
├── obtains canonical external result
└── calls Payment operation
        ↓
Payment operation
├── invokes policy or pure Domain Service
├── receives immutable decision
├── validates legal transition
├── mutates itself atomically
└── registers ordered domain events
```

A Domain Service is never an orchestration service and never a port adapter.

## 4. Policy profiles

Every configurable decision uses an immutable approved profile containing:

```text
profileId
profileVersion
effectiveFrom
effectiveUntil?
approvedByReference
```

The application selects the effective profile. The domain never fetches it.

No bank-specific timeout, freshness duration, algorithm list, check list,
reversal authorization rule or disclosure allowlist is invented in Java.

## 5. Policy catalogue

| ID | Policy | Kind | Output | Commands |
| --- | --- | --- | --- | --- |
| `PAY-POL-001` | `EvidenceTemporalValidityPolicy` | `DOMAIN_POLICY` | `EvidenceTemporalDecision` | `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-010` |
| `PAY-POL-002` | `AuthorizationEvidenceAcceptancePolicy` | `DOMAIN_POLICY` | `EvidenceAcceptanceDecision` | `PAY-CMD-003` |
| `PAY-POL-003` | `BankingVerificationAcceptancePolicy` | `DOMAIN_POLICY` | `EvidenceAcceptanceDecision` | `PAY-CMD-004` |
| `PAY-POL-004` | `FundsControlAcceptancePolicy` | `DOMAIN_POLICY` | `EvidenceAcceptanceDecision` | `PAY-CMD-005`, `PAY-CMD-007` |
| `PAY-POL-005` | `TreasuryResolutionAcceptancePolicy` | `DOMAIN_POLICY` | `EvidenceAcceptanceDecision` | `PAY-CMD-006`, `PAY-CMD-007` |
| `PAY-POL-006` | `EvidenceReplayReplacementPolicy` | `DOMAIN_POLICY` | `EvidenceReplayDecision` | `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-012`, `PAY-CMD-013` |
| `PAY-POL-007` | `PostingInstructionAuthorizationPolicy` | `DOMAIN_POLICY` | `PostingAuthorizationDecision` | `PAY-CMD-007` |
| `PAY-POL-008` | `PostingOutcomeInterpretationPolicy` | `DOMAIN_POLICY` | `PostingOutcomeInterpretation` | `PAY-CMD-008`, `PAY-CMD-009` |
| `PAY-POL-009` | `EndOfDayConfirmationAcceptancePolicy` | `DOMAIN_POLICY` | `EndOfDayInterpretation` | `PAY-CMD-010` |
| `PAY-POL-010` | `ReversalAuthorizationPolicy` | `DOMAIN_POLICY` | `ReversalAuthorizationDecision` | `PAY-CMD-011` |
| `PAY-POL-011` | `ReversalOutcomeInterpretationPolicy` | `DOMAIN_POLICY` | `ReversalOutcomeInterpretation` | `PAY-CMD-012`, `PAY-CMD-013` |
| `PAY-POL-012` | `FailureClassificationPolicy` | `DOMAIN_POLICY` | `FailureDispositionDecision` | `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-012`, `PAY-CMD-013`, `PAY-CMD-014`, `PAY-CMD-015`, `PAY-CMD-016` |
| `PAY-POL-013` | `PaymentResultIntentPolicy` | `DOMAIN_POLICY` | `ResultIntentDecision` | `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-012`, `PAY-CMD-013`, `PAY-CMD-014`, `PAY-CMD-015`, `PAY-CMD-016` |
| `PAY-POL-014` | `PaymentEventDisclosurePolicy` | `BOUNDARY_POLICY` | `EventDisclosureDecision` | Outbox mapping |

## 6. Detailed policies

### `PAY-POL-001` — `EvidenceTemporalValidityPolicy`

Validate structural chronology, future-dating tolerance, freshness and explicit validity windows for canonical evidence.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceTemporalDecision`
- **Profile:** `EvidenceTemporalProfile`
- **Invariants:** `PAY-INV-009`, `PAY-INV-022`, `PAY-INV-032`

**Inputs**

- `EvidenceMetadata`
- `Instant decisionAt`
- `EvidenceTemporalProfile`

**Forbidden**

- Read a system clock.
- Invent a freshness duration.
- Convert a stale result into approval.

### `PAY-POL-002` — `AuthorizationEvidenceAcceptancePolicy`

Determine whether minimized authorization evidence is structurally acceptable and correctly bound to the immutable Payment request.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceAcceptanceDecision`
- **Profile:** `AuthorizationPolicyProfile`
- **Invariants:** `PAY-INV-021`, `PAY-INV-022`, `PAY-INV-023`, `PAY-INV-024`, `PAY-INV-025`

**Inputs**

- `PaymentAuthorizationContext`
- `AuthorizationEvidenceSnapshot`
- `Instant decisionAt`
- `AuthorizationPolicyProfile`

**Forbidden**

- Verify a JWT signature or download JWKS.
- Read raw claims or credentials.
- Fabricate an authorization result after infrastructure failure.

### `PAY-POL-003` — `BankingVerificationAcceptancePolicy`

Validate banking-verification source, required checks, institution/account binding, outcome semantics and freshness.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceAcceptanceDecision`
- **Profile:** `BankingVerificationPolicyProfile`
- **Invariants:** `PAY-INV-027`, `PAY-INV-028`, `PAY-INV-029`, `PAY-INV-030`, `PAY-INV-031`

**Inputs**

- `PaymentBankingContext`
- `BankingVerificationSnapshot`
- `Instant decisionAt`
- `BankingVerificationPolicyProfile`

**Forbidden**

- Query Amplitude.
- Read ObservedCustomer as approval authority.
- Inspect KYC values or customer identity payloads.

### `PAY-POL-004` — `FundsControlAcceptancePolicy`

Validate that funds evidence is favorable, fresh and bound to the exact Payment amount, currency, bank and debtor account.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceAcceptanceDecision`
- **Profile:** `FundsControlPolicyProfile`
- **Invariants:** `PAY-INV-027`, `PAY-INV-032`, `PAY-INV-033`, `PAY-INV-034`

**Inputs**

- `PaymentFundsContext`
- `FundsControlSnapshot`
- `Instant decisionAt`
- `FundsControlPolicyProfile`

**Forbidden**

- Query balances.
- Persist available funds.
- Extend or infer validUntil.

### `PAY-POL-005` — `TreasuryResolutionAcceptancePolicy`

Validate protected CUT/Treasury resolution against Payment bank, allocation fingerprint and approved configuration identity/version.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceAcceptanceDecision`
- **Profile:** `TreasuryResolutionPolicyProfile`
- **Invariants:** `PAY-INV-035`, `PAY-INV-036`, `PAY-INV-037`, `PAY-INV-038`

**Inputs**

- `PaymentTreasuryContext`
- `TreasuryAccountResolutionSnapshot`
- `TreasuryAccountReference?`
- `Instant decisionAt`
- `TreasuryResolutionPolicyProfile`

**Forbidden**

- Read protected configuration.
- Create a Treasury account from inbound TRESOR PAY data.
- Expose the account integration token.

### `PAY-POL-006` — `EvidenceReplayReplacementPolicy`

Classify new, identical, more authoritative, conflicting or quarantinable evidence without mutating Payment.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EvidenceReplayDecision`
- **Invariants:** `PAY-INV-025`, `PAY-INV-037`, `PAY-INV-042`, `PAY-INV-058`, `PAY-INV-069`

**Inputs**

- `CurrentEvidenceIdentity?`
- `CandidateEvidenceIdentity`
- `EvidenceAuthority`
- `EvidenceConclusiveness`
- `PaymentLifecycleContext`

**Forbidden**

- Persist audit or quarantine records.
- Overwrite terminal evidence.
- Treat different fingerprints as a replay.

### `PAY-POL-007` — `PostingInstructionAuthorizationPolicy`

Authorize the sole logical posting instruction only when all favorable evidence is coherent, fresh and immutable.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `PostingAuthorizationDecision`
- **Profile:** `PostingAuthorizationPolicyProfile`
- **Invariants:** `PAY-INV-032`, `PAY-INV-035`, `PAY-INV-037`, `PAY-INV-038`, `PAY-INV-039`, `PAY-INV-040`, `PAY-INV-041`

**Inputs**

- `PaymentPostingAuthorizationContext`
- `PostingInstructionIdentity`
- `Instant decisionAt`
- `PostingAuthorizationPolicyProfile`

**Forbidden**

- Create the posting instruction identity.
- Call Amplitude.
- Approve a second logical posting.
- Authorize from stale funds evidence.

### `PAY-POL-008` — `PostingOutcomeInterpretationPolicy`

Validate leg/reference combinations and map canonical posting evidence to a typed financial decision.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `PostingOutcomeInterpretation`
- **Profile:** `FinancialOutcomePolicyProfile`
- **Invariants:** `PAY-INV-041`, `PAY-INV-042`, `PAY-INV-043`, `PAY-INV-044`, `PAY-INV-045`, `PAY-INV-046`, `PAY-INV-047`, `PAY-INV-048`, `PAY-INV-049`, `PAY-INV-050`

**Inputs**

- `PaymentPostingContext`
- `PostingOutcomeSnapshot`
- `PaymentFailure?`
- `FinancialOutcomePolicyProfile`

**Forbidden**

- Perform lookup or posting.
- Assume UNKNOWN means failure.
- Establish TFJ finality.
- Delete the original posting identity.

### `PAY-POL-009` — `EndOfDayConfirmationAcceptancePolicy`

Validate a durably matched TFJ confirmation and map INTEGRATED or FAILED recovery semantics without performing the match.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `EndOfDayInterpretation`
- **Profile:** `TfjPolicyProfile`
- **Invariants:** `PAY-INV-053`, `PAY-INV-054`, `PAY-INV-055`, `PAY-INV-056`, `PAY-INV-057`, `PAY-INV-058`

**Inputs**

- `PaymentTfjContext`
- `EndOfDayConfirmationSnapshot`
- `UniqueTfjMatchProof`
- `Instant decisionAt`
- `TfjPolicyProfile`

**Forbidden**

- Query TFJ.
- Persist or search unmatched confirmations.
- Accept PENDING.
- Map provider FAILED automatically to Payment FAILED.

### `PAY-POL-010` — `ReversalAuthorizationPolicy`

Determine whether a confirmed financial effect is eligible for one explicitly authorized reversal instruction.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `ReversalAuthorizationDecision`
- **Profile:** `ReversalPolicyProfile`
- **Invariants:** `PAY-INV-059`, `PAY-INV-060`, `PAY-INV-064`

**Inputs**

- `PaymentReversalEligibilityContext`
- `ReversalInstructionIdentity`
- `ReversalAuthorizationEvidence`
- `Instant decisionAt`
- `ReversalPolicyProfile`

**Forbidden**

- Approve an actor or runbook.
- Call the bank.
- Reuse the posting idempotency key.
- Authorize reversal without confirmed/reconciled effect.

### `PAY-POL-011` — `ReversalOutcomeInterpretationPolicy`

Validate canonical reversal evidence and map REVERSED, UNKNOWN, REJECTED or NOT_ALLOWED to a typed decision.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `ReversalOutcomeInterpretation`
- **Profile:** `FinancialOutcomePolicyProfile`
- **Invariants:** `PAY-INV-060`, `PAY-INV-061`, `PAY-INV-062`, `PAY-INV-063`, `PAY-INV-064`

**Inputs**

- `PaymentReversalContext`
- `ReversalSnapshot`
- `PaymentFailure?`
- `FinancialOutcomePolicyProfile`

**Forbidden**

- Perform reversal lookup or resubmission.
- Erase the original financial effect after rejection.
- Treat UNKNOWN as REVERSED or FAILED.

### `PAY-POL-012` — `FailureClassificationPolicy`

Classify stable PaymentFailure plus proven financial-effect knowledge into reject, fail, defer, unknown or reversal-required semantics.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `FailureDispositionDecision`
- **Profile:** `FailureClassificationProfile`
- **Invariants:** `PAY-INV-024`, `PAY-INV-029`, `PAY-INV-033`, `PAY-INV-043`, `PAY-INV-057`, `PAY-INV-062`, `PAY-INV-064`, `PAY-INV-065`, `PAY-INV-066`, `PAY-INV-067`, `PAY-INV-068`

**Inputs**

- `PaymentFailure`
- `FinancialEffectKnowledge`
- `PaymentStatus`
- `FailureClassificationProfile`

**Forbidden**

- Parse free-form provider text as a business decision.
- Return FAILED when financial effect is uncertain.
- Reveal technical diagnostics.

### `PAY-POL-013` — `PaymentResultIntentPolicy`

Determine whether an accepted mutation requires an immediate, final or reversal result intent and its safe semantic result type.

- **Kind:** `DOMAIN_POLICY`
- **Conceptual package:** `com.sixpay.payment.domain.policy`
- **Output:** `ResultIntentDecision`
- **Profile:** `ResultIntentPolicyProfile`
- **Invariants:** `PAY-INV-051`, `PAY-INV-052`, `PAY-INV-053`, `PAY-INV-057`, `PAY-INV-068`

**Inputs**

- `PaymentResultContext`
- `PaymentStatus previousStatus`
- `PaymentStatus resultingStatus`
- `PaymentFailure?`
- `Instant availableAt`
- `ResultIntentPolicyProfile`

**Forbidden**

- Deliver a notification.
- Use notification outcome to change Payment state.
- Expose a full failure or snapshot.

### `PAY-POL-014` — `PaymentEventDisclosurePolicy`

Validate the explicit Outbox payload against the event catalogue, classification rules and sensitive-data denylist.

- **Kind:** `BOUNDARY_POLICY`
- **Conceptual package:** `com.sixpay.payment.application.policy`
- **Output:** `EventDisclosureDecision`
- **Profile:** `EventDisclosureProfile`
- **Invariants:** `PAY-INV-011`, `PAY-INV-012`, `PAY-INV-013`, `PAY-INV-016`, `PAY-INV-018`

**Inputs**

- `PaymentDomainEvent`
- `ExplicitEventPayload`
- `EventDisclosureProfile`

**Forbidden**

- Serialize the aggregate or snapshot automatically.
- Decrypt or retrieve protected account material.
- Publish a payload not declared in the event catalogue.

## 7. Domain Service catalogue

| ID | Service | Output | Policies | Commands |
| --- | --- | --- | --- | --- |
| `PAY-DS-001` | `PostingOutcomeDecisionService` | `PostingDecision` | `PAY-POL-006`, `PAY-POL-008`, `PAY-POL-012` | `PAY-CMD-008`, `PAY-CMD-009` |
| `PAY-DS-002` | `EndOfDayDecisionService` | `EndOfDayDecision` | `PAY-POL-006`, `PAY-POL-009`, `PAY-POL-012` | `PAY-CMD-010` |
| `PAY-DS-003` | `ReversalDecisionService` | `ReversalDecision` | `PAY-POL-006`, `PAY-POL-010`, `PAY-POL-011`, `PAY-POL-012` | `PAY-CMD-011`, `PAY-CMD-012`, `PAY-CMD-013` |
| `PAY-DS-004` | `PaymentResultIntentService` | `ResultIntentDecision` | `PAY-POL-013` | `PAY-CMD-003`, `PAY-CMD-004`, `PAY-CMD-005`, `PAY-CMD-006`, `PAY-CMD-008`, `PAY-CMD-009`, `PAY-CMD-010`, `PAY-CMD-012`, `PAY-CMD-013`, `PAY-CMD-014`, `PAY-CMD-015`, `PAY-CMD-016` |

## 8. Detailed Domain Services

### `PAY-DS-001` — `PostingOutcomeDecisionService`

Coordinate evidence replay, structural posting interpretation and failure classification into one immutable PostingDecision.

- **Output:** `PostingDecision`
- **Policies:** `PAY-POL-006`, `PAY-POL-008`, `PAY-POL-012`
- **Invariants:** `PAY-INV-041`, `PAY-INV-042`, `PAY-INV-043`, `PAY-INV-044`, `PAY-INV-045`, `PAY-INV-046`, `PAY-INV-047`, `PAY-INV-048`, `PAY-INV-049`, `PAY-INV-050`

**Conceptual inputs**

- `PaymentPostingContext`
- `PostingOutcomeSnapshot candidate`
- `PaymentFailure?`
- `CurrentPostingEvidence?`
- `Instant decisionAt`
- `PaymentPolicyBundle`

**Forbidden**

- Mutate Payment.
- Register events.
- Execute or look up posting.
- Choose a new posting identity.

### `PAY-DS-002` — `EndOfDayDecisionService`

Coordinate replay, unique-match proof, TFJ interpretation and failure classification into one immutable EndOfDayDecision.

- **Output:** `EndOfDayDecision`
- **Policies:** `PAY-POL-006`, `PAY-POL-009`, `PAY-POL-012`
- **Invariants:** `PAY-INV-053`, `PAY-INV-054`, `PAY-INV-055`, `PAY-INV-056`, `PAY-INV-057`, `PAY-INV-058`

**Conceptual inputs**

- `PaymentTfjContext`
- `EndOfDayConfirmationSnapshot candidate`
- `UniqueTfjMatchProof`
- `PaymentFailure?`
- `CurrentTfjEvidence?`
- `Instant decisionAt`
- `PaymentPolicyBundle`

**Forbidden**

- Perform TFJ matching or lookup.
- Persist quarantine.
- Mutate Payment.
- Register events.

### `PAY-DS-003` — `ReversalDecisionService`

Coordinate authorization or canonical reversal outcome interpretation with replay and failure rules.

- **Output:** `ReversalDecision`
- **Policies:** `PAY-POL-006`, `PAY-POL-010`, `PAY-POL-011`, `PAY-POL-012`
- **Invariants:** `PAY-INV-059`, `PAY-INV-060`, `PAY-INV-061`, `PAY-INV-062`, `PAY-INV-063`, `PAY-INV-064`

**Conceptual inputs**

- `PaymentReversalContext`
- `ReversalDecisionInput`
- `CurrentReversalEvidence?`
- `Instant decisionAt`
- `PaymentPolicyBundle`

**Forbidden**

- Authorize the human/operator.
- Submit or look up reversal.
- Mutate Payment.
- Register events.

### `PAY-DS-004` — `PaymentResultIntentService`

Build the typed result-intent decision associated with an accepted Payment mutation.

- **Output:** `ResultIntentDecision`
- **Policies:** `PAY-POL-013`
- **Invariants:** `PAY-INV-051`, `PAY-INV-052`, `PAY-INV-053`, `PAY-INV-057`, `PAY-INV-068`

**Conceptual inputs**

- `PaymentResultContext`
- `PaymentStatus previousStatus`
- `PaymentStatus resultingStatus`
- `PaymentFailure?`
- `Instant availableAt`
- `PaymentPolicyBundle`

**Forbidden**

- Deliver Notification.
- Change Payment financial state.
- Read external data.
- Register the event itself.

## 9. Aggregate integration

### Evidence operations owned directly by Payment

Payment invokes the corresponding acceptance and replay policies for:

```text
recordAuthorizationDecision
recordBankingVerification
recordFundsControl
recordTreasuryAccountResolution
authorizePosting
```

These decisions remain close to the aggregate because they govern one current
transition and one current evidence category.

### Cross-object interpretations delegated to services

```text
recordPostingOutcome / resolvePostingOutcome
    → PostingOutcomeDecisionService

recordMatchedEndOfDayConfirmation
    → EndOfDayDecisionService

authorizeReversal / recordReversalOutcome / resolveReversalOutcome
    → ReversalDecisionService

all accepted result-producing transitions
    → PaymentResultIntentService
```

The aggregate consumes the returned decision and performs the mutation itself.

## 10. Components that are not Domain Services

The following remain outside the domain:

- policy configuration loading;
- JWT/JWKS verification;
- Amplitude customer/account and funds calls;
- protected Treasury configuration lookup;
- posting and reversal execution;
- posting and reversal outcome lookup;
- TFJ persistence, matching and quarantine;
- repositories and command registry;
- secure instruction-material retrieval;
- Outbox and event publication;
- retries, circuit breakers, DLQ and runbooks;
- system clock access.

Calling one of these a Domain Service would hide I/O and violate the Payment
boundary.

## 11. Time and freshness

The domain never calls `Instant.now()`.

The handler provides one controlled `decisionAt` used by:

- the aggregate operation;
- evidence temporal policies;
- resulting timestamps;
- domain event occurrence time.

Freshness values come from an approved versioned profile. `validUntil` supplied
by an authoritative snapshot remains binding and cannot be extended by policy.

## 12. Configuration and audit

When a profile materially influences a decision, its identity/version is
retained in audit or canonical evidence.

Profile changes affect only future decisions after their effective date. They
never reinterpret terminal Payment history.

Secrets are never policy configuration.

## 13. Test strategy

| Component | Minimum verification |
| --- | --- |
| Policy | Pure unit decision table |
| Temporal / replay policy | Boundary and property-based tests |
| Posting service | Full outcome × leg × failure matrix |
| TFJ service | Match/status/recovery matrix |
| Reversal service | Authorization/outcome/replay matrix |
| Result-intent service | Transition-to-result matrix |
| Disclosure policy | Event field allowlist and sensitive-data tests |
| Profile loading | Application integration test |
| External ports | Contract and resilience tests outside domain |

## 14. Decisions closed in Lot 2.7

- `PAY-DEC-IA1-051` — `POLICY_DOMAIN_SERVICE_PORT_AND_INFRASTRUCTURE_RESPONSIBILITIES_ARE_EXPLICITLY_SEPARATED`
- `PAY-DEC-IA1-052` — `ALL_DOMAIN_POLICIES_AND_SERVICES_ARE_PURE_DETERMINISTIC_AND_RECEIVE_TIME_CONFIGURATION_EXPLICITLY`
- `PAY-DEC-IA1-053` — `FOURTEEN_POLICIES_AND_FOUR_PURE_DOMAIN_SERVICES_FORM_THE_PAYMENT_DECISION_MODEL`
- `PAY-DEC-IA1-054` — `PAYMENT_AGGREGATE_REMAINS_SOLE_STATE_AND_EVENT_OWNER_DOMAIN_SERVICES_RETURN_DECISIONS_ONLY`
- `PAY-DEC-IA1-055` — `POLICY_PROFILES_ARE_VERSIONED_APPROVED_AND_INJECTED_NO_BANK_CONSTANT_IS_INVENTED_OR_HARD_CODED`
- `PAY-DEC-IA1-056` — `PERSISTENT_MATCHING_LOOKUP_RETRY_CIRCUIT_BREAKER_DLQ_AND_SECRET_ACCESS_REMAIN_OUTSIDE_DOMAIN`
- `PAY-DEC-IA1-057` — `EVIDENCE_REPLAY_REPLACEMENT_AND_CONFLICT_CLASSIFICATION_USE_ONE_SHARED_DOMAIN_POLICY`
- `PAY-DEC-IA1-058` — `POSTING_TFJ_AND_REVERSAL_SERVICES_RETURN_IMMUTABLE_DECISIONS_WITHOUT_MUTATION_EVENT_OR_IO`
- `PAY-DEC-IA1-059` — `RESULT_INTENT_DECISION_AND_EVENT_PAYLOAD_DISCLOSURE_ARE_SEPARATE_POLICIES`
- `PAY-DEC-IA1-060` — `POLICY_AND_SERVICE_DEFINITIONS_DO_NOT_AUTHORIZE_JAVA_GENERATION_BEFORE_FINAL_MODEL_VALIDATION`
## 15. Exit checklist

- [ ] Every configurable decision has a named profile.
- [ ] No profile contains a secret or protected account.
- [ ] Every policy is pure and deterministic.
- [ ] Every Domain Service returns a typed immutable decision.
- [ ] Payment remains the only state/event owner.
- [ ] Every command and invariant references its required policies/services.
- [ ] State-machine policy/service references are consistent.
- [ ] Event disclosure is separated from result-intent selection.
- [ ] Persistent matching and external calls remain ports/processes.
- [ ] No bank-specific constant has been invented.
- [ ] Code generation remains forbidden.

## 16. Verdict

```text
IA-1 LOT 2.7 POLICIES AND DOMAIN SERVICES PREPARED
POLICY COUNT: 14
PURE DOMAIN SERVICE COUNT: 4
CONFIGURATION PROFILE COUNT: 12
STATUS: DRAFT_PENDING_VALIDATION
NEXT: LOT 2.8 — FINAL MODEL VALIDATION
CODE GENERATION: FORBIDDEN
```
