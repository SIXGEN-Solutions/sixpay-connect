# SIXPAY CONNECT — Payment Domain Kernel Final Validation

> **Lot:** `3.10 — Validation finale du noyau de domaine`  
> **Status:** `IMPLEMENTED`  
> **Scope:** `PAYMENT_DOMAIN_ONLY`

## Validated counts

| Element | Required | Validated |
| --- | ---: | ---: |
| Payment states | 17 | 17 |
| Named aggregate operations | 17 | 17 |
| Legal transitions | 38 | 38 |
| Invariants | 76 | 76 |
| Domain events | 33 | 33 |
| Policies | 14 | 14 |
| Domain Services | 4 | 4 |
| Terminal states | 4 | 4 |

## Validation strategy

The lot does not recreate `Payment`, its Policies or its events. It adds a
machine-verifiable consistency layer between:

- Java implementation;
- state-machine catalogue;
- invariant catalogue;
- event catalogue;
- command/operation catalogue;
- architecture tests;
- acceptance traceability.

## Structural alignment

`PaymentDomainException` is moved from `domain.model` to the canonical:

```text
com.sixpay.payment.domain.exception
```

No business semantics are changed.

## Mandatory guarantees

- terminal states have no outgoing transition;
- rejected operations and identical replays do not increment the version;
- event version and sequence remain mutation-local and ordered;
- no complete aggregate or evidence snapshot is exposed in events;
- the domain remains free of Spring, JPA, infrastructure and I/O;
- all normative identifiers remain `PAY-*` traceable.

## Verdict

```text
PAYMENT DOMAIN KERNEL: VALIDATED
VERTICAL MODULE LAYERS: STILL DEFERRED
GLOBAL GENERATION: FORBIDDEN
```
