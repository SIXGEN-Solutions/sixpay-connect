# Architecture Decision Record — Payment Evidence Snapshots

## Decision

Lot 3.3 implements the seven immutable, minimized evidence categories accepted
by the future Payment Aggregate Root.

## Package

```text
com.sixpay.payment.domain.model.evidence
```

All support identifiers, classifications and evidence structures needed by
these snapshots are colocated in this package.

## Structural versus contextual validation

Implemented in constructors:

- valid IDs and fingerprints;
- source/channel compatibility;
- time chronology;
- immutable bounded collections;
- outcome consistency;
- posting leg/reference consistency;
- TFJ final-status shape;
- reversal authorization before submission;
- immutable reversal identity preservation.

Deferred to Policies/Aggregate Root:

- approved issuer/algorithm/scope profiles;
- mandatory banking/funds check sets;
- exact Payment amount/account/bank binding;
- freshness windows;
- same-evidence replay and replacement ranking;
- unique TFJ match proof;
- lifecycle eligibility and state transition;
- event generation.

## Confidentiality

Full snapshots are explicit final classes with safe `toString()` methods.
Protected tokens, account fingerprints and full check details are not emitted
by default string representations.

## Instruction identities

`PostingInstructionId`, `PostingIdempotencyKey`,
`ReversalInstructionId` and `ReversalIdempotencyKey` are implemented because
posting and reversal evidence cannot be structurally identified without them.

Full `PostingInstructionIdentity` and `ReversalInstructionIdentity` composites
remain deferred.

## Controlled authorization

```text
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_3_SNAPSHOTS_FINANCIAL_EVIDENCE
currentIncrementCodeGenerationAllowed: true
globalCodeGenerationAllowed: false
```
