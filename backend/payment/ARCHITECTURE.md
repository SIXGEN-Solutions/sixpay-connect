# Architecture Decision Record — Payment Policies and Services

## Decision

Lot 3.4 implements the complete IA-1 decision model as pure Java 21 domain
components.

## Packages

```text
com.sixpay.payment.domain.policy
com.sixpay.payment.domain.service
```

## Ownership boundary

Policies and services decide. The future `Payment` Aggregate Root will:

- validate legal transitions;
- mutate its own state;
- retain accepted evidence;
- register ordered domain events.

No Lot 3.4 component owns those responsibilities.

## Profiles

The twelve profiles are immutable and carry common metadata:

```text
profileId
profileVersion
effectiveFrom
effectiveUntil?
approvedByReference
```

No profile contains credentials, protected account tokens or a hard-coded bank
default. Values are supplied by the application after approved configuration
resolution.

## Event disclosure boundary

The final catalogue places `PaymentEventDisclosurePolicy` conceptually near
application mapping. Because the active authorization is domain-only, the
implementation remains a pure boundary policy over `ExplicitEventPayload`.
No application package or Outbox mapper is generated.

## Determinism

For the same input contexts, evidence, `decisionAt` and profile versions, every
policy and service returns the same typed result.
