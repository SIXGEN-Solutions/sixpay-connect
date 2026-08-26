# SIXPAY CONNECT — Phase 3 / Lot 3.6

## Application Layer

| Metadata | Value |
| --- | --- |
| Authoritative branch | `feat/backend-payment` |
| Golden Module | `partner` |
| Controllers | Forbidden |
| Spring annotations | Forbidden in application contracts |
| External banking contracts | Deferred to Lot 3.8 |

## Repository finding

The authoritative branch contains only:

```text
application/package-info.java
```

and no implemented Payment commands, queries, views or use-case ports.

The previously generated Lot 3.7 service ZIP was not present on the branch at
analysis time. This delivery therefore establishes the application contracts
first, in the correct dependency order.

## Structure

```text
application/
├── command/
├── query/
├── view/
└── port/
    ├── in/
    └── out/
```

## Commands

Commands correspond one-to-one with named Payment Aggregate operations. They
are immutable records and carry already constructed domain inputs.

They do not repeat business validation. The Aggregate Root remains the owner
of status transitions, evidence acceptance and policy decisions.

## Queries

The initial query surface supports lookup by:

- Payment ID;
- public Payment reference;
- source plus external Payment reference.

No pagination or search filter is invented before the internal query contract
is implemented.

## Views

`PaymentView` deliberately excludes:

- debtor-account data;
- complete evidence snapshots;
- authorization secrets;
- posting or reversal payloads.

Transport adapters may later map it to approved API contracts.

## Inbound ports

Use cases are split by workflow:

```text
PaymentReceptionUseCase
PaymentAuthorizationUseCase
PaymentFundsControlUseCase
PaymentTreasuryResolutionUseCase
PaymentPostingPreparationUseCase
PaymentFinalizationUseCase
PaymentQueryUseCase
```

No monolithic Payment use case is introduced.

## Outbound ports

Only two technical boundaries are introduced:

```text
PaymentLookupPort
PaymentAtomicPersistencePort
```

The atomic port represents:

```text
Payment + Audit + Outbox
```

inside one transaction.

Banking, Partner, Customer, TFJ and notification ports are deferred to Lot 3.8
because their exact contracts must be derived from repository contracts rather
than invented.

## No Controller

This lot creates no REST controller, listener, consumer or transport adapter.

## Follow-up required for Lot 3.7

The orchestration services must implement these inbound interfaces and replace
domain-oriented public method signatures with command-based signatures.

The existing persistence/audit/outbox components must be composed behind the
outbound ports without moving business rules into infrastructure.

## Validation

From `backend/`:

```powershell
mvnw.cmd clean verify -pl payment -am
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```
