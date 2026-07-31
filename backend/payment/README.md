# SIXPAY CONNECT — Payment Module

## Current increment

```text
Lot 3.4 — Policies and Domain Services
Scope: PAYMENT_DOMAIN_ONLY
Global generation: FORBIDDEN
```

## Implemented decision model

```text
14 pure policies
12 immutable injected profiles
4 pure Domain Services
typed decision enums and records
```

### Policies

```text
EvidenceTemporalValidityPolicy
AuthorizationEvidenceAcceptancePolicy
BankingVerificationAcceptancePolicy
FundsControlAcceptancePolicy
TreasuryResolutionAcceptancePolicy
EvidenceReplayReplacementPolicy
PostingInstructionAuthorizationPolicy
PostingOutcomeInterpretationPolicy
EndOfDayConfirmationAcceptancePolicy
ReversalAuthorizationPolicy
ReversalOutcomeInterpretationPolicy
FailureClassificationPolicy
PaymentResultIntentPolicy
PaymentEventDisclosurePolicy
```

### Domain Services

```text
PostingOutcomeDecisionService
EndOfDayDecisionService
ReversalDecisionService
PaymentResultIntentService
```

## Purity

No component performs I/O, repository access, external calls, persistence,
clock reads, aggregate mutation or event registration.

`decisionAt` and immutable approved profiles are explicit method inputs.

## Deferred

`Payment`, `PaymentState`, domain events, handlers, repositories, Outbox and
adapters remain absent.

## Build

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am test
```
