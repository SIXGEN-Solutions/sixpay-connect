# SIXPAY CONNECT — Phase 3 / Lot 3.7

## Payment Orchestration

| Metadata | Value |
| --- | --- |
| Authoritative branch | `feat/backend-payment` |
| Domain Aggregate | `Payment` |
| Transactional writes | Payment + Audit + Outbox |
| External gateways | Deferred to Lot 3.8 |
| Kafka publication | Forbidden |
| Monolithic service | Forbidden |

## Repository finding

At the beginning of this lot, the branch contains the infrastructure
foundations from Lots 3.2 through 3.5, but no usable Payment application ports,
commands or workflow services.

This delivery therefore introduces only the minimum application result and
service contracts required for orchestration. It does not claim that the full
Lot 3.6 API-facing application layer is complete.

## Service decomposition

```text
PaymentReceptionService
PaymentAuthorizationService
PaymentFundsControlService
PaymentTreasuryResolutionService
PaymentPostingPreparationService
PaymentFinalizationService
```

Each service owns a coherent workflow stage and delegates lifecycle decisions
to the Payment Aggregate Root.

No service duplicates transition rules, policy evaluation or evidence
acceptance.

## Shared transaction coordinator

`PaymentMutationCoordinator` centralizes only the repeated mechanical flow:

```text
load or receive Payment
→ invoke one domain operation
→ detect domain no-op through businessVersion
→ persist Payment
→ append audit events
→ stage outbox events
→ commit atomically
```

The coordinator contains no business transition decision.

## No external side effects

This lot deliberately does not call:

- Core Banking or Amplitude;
- Partner or Customer;
- TFJ source;
- Kafka;
- notification;
- HTTP endpoints.

Services consume evidence already obtained by a future inbound adapter or
gateway.

## Event atomicity

Every changed aggregate must expose at least one new Payment domain event.
The transaction stores:

```text
payments
payment_audit
payment_outbox_events
```

before commit.

An idempotent domain no-op does not rewrite the aggregate and does not create
new audit or outbox rows.

## Lot 3.6 dependency

A complete command/query/use-case port surface remains a separate prerequisite
for REST or messaging adapters. This lot intentionally avoids inventing API
contracts that are not yet implemented in the branch.

## Validation

From `backend/` on Windows:

```powershell
mvnw.cmd clean verify -pl payment -am
mvnw.cmd clean verify -Pfull-tests -pl payment -am
```
