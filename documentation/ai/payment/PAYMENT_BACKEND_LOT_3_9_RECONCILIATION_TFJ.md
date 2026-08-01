# SIXPAY CONNECT — Phase 3 / Lot 3.9

## Reconciliation / TFJ

### Core rule

```text
Posting != Reconciliation
```

Posting establishes the immediate Core Banking outcome.

Reconciliation establishes whether the posted financial effect is present in
the authoritative end-of-day TFJ evidence and whether Treasury integration is
final.

### Repository correction

Before this lot, TFJ reconciliation was mixed into:

```text
PaymentFinalizationService
PaymentFinalizationUseCase
```

alongside posting outcomes and reversal.

This lot removes that responsibility and introduces:

```text
PaymentReconciliationService
PaymentReconciliationUseCase
```

### Posting responsibilities

Posting owns:

- recording the direct Core Banking outcome;
- authoritative lookup of an uncertain posting outcome;
- movement to `POSTED_PENDING_TFJ` after confirmed posting;
- reversal processing when separately authorized.

Posting does not determine TFJ finality.

### Reconciliation responsibilities

Reconciliation owns only:

- accepting an end-of-day confirmation;
- requiring a unique TFJ match proof;
- evaluating the evidence through the Payment Aggregate Root;
- transitioning to `TREASURY_INTEGRATED`;
- requiring reversal when TFJ proves a financial inconsistency;
- retaining `POSTED_PENDING_TFJ` and emitting a reconciliation-required event
  when the evidence remains unresolved.

### Transaction boundary

Applying one TFJ result remains an aggregate mutation and therefore persists:

```text
Payment + Audit + Outbox
```

atomically through `PaymentMutationCoordinator`.

It does not share a transaction with the original bank posting because TFJ is
an asynchronous, later source of truth.

### Deliberately deferred

No TFJ file parser, batch reader, scheduler, SFTP adapter or reconciliation
table is generated here because no approved ingestion contract was found in
the authoritative branch.

The current command accepts an already constructed:

```text
EndOfDayConfirmationSnapshot
UniqueTfjMatchProof
```

A later TFJ ingestion lot may create source adapters without changing the
separation introduced here.
