# SIXPAY CONNECT — Payment Module

## Status

```text
Current increment: Lot 3.3 — Snapshots and financial evidence
Implementation scope: PAYMENT_DOMAIN_ONLY
Global generation: FORBIDDEN
Current increment generation: AUTHORIZED
```

## Implemented evidence

```text
AuthorizationEvidenceSnapshot
BankingVerificationSnapshot
FundsControlSnapshot
TreasuryAccountResolutionSnapshot
PostingOutcomeSnapshot
EndOfDayConfirmationSnapshot
ReversalSnapshot
```

The module now contains immutable support types for:

- common source/correlation/channel/fingerprint metadata;
- authorization bindings;
- banking and funds check evidence;
- protected Treasury resolution;
- posting instruction identity, leg outcomes and next actions;
- uniquely matched TFJ finality evidence;
- reversal authorization and canonical outcome evidence.

## Structural boundary

Constructors enforce only the structure defined by the frozen IA-1 model:

- chronology;
- source and channel compatibility;
- bounded unique check collections;
- outcome/field consistency;
- positive amounts;
- principal and leg reference consistency;
- immutable preservation of original reversal identities.

Freshness thresholds, mandatory-check profiles, binding against a particular
Payment, evidence replacement authority and lifecycle eligibility remain
Policies or Aggregate Root concerns.

## Prohibited behavior

Evidence types do not:

- call Security, Amplitude or Accounting;
- verify cryptographic signatures;
- load configuration;
- read the system clock;
- perform persistence;
- mutate Payment;
- publish events.

## Tests

```text
EvidenceMetadataTest
AuthorizationEvidenceSnapshotTest
BankingAndFundsSnapshotsTest
TreasuryAndPostingSnapshotsTest
EndOfDayAndReversalSnapshotsTest
PaymentArchitectureTest
```

## Next increment

```text
Lot 3.4 — Policies and pure Domain Services
```

The Aggregate Root and `PaymentState` remain deferred.
