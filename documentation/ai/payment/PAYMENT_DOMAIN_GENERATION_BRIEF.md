# SIXPAY CONNECT — Payment Domain Generation Brief

> **Current lot:** `3.4 — Policies and Domain Services`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Status:** `LOT_3_4_IMPLEMENTED`  
> **Global code generation:** **FORBIDDEN**  
> **Current increment:** **AUTHORIZED**

## Implemented

```text
14 Payment Policies
12 immutable policy profiles
4 pure Domain Services
typed decision enums and immutable decision records
explicit decision contexts and service inputs
```

## Purity guarantees

Every policy and service:

- receives time and profiles explicitly;
- performs no I/O;
- accesses no repository;
- calls no external client;
- reads no system clock;
- mutates no Payment;
- registers no event;
- returns a typed immutable decision.

## Packages

```text
com.sixpay.payment.domain.policy
com.sixpay.payment.domain.service
```

`PaymentEventDisclosurePolicy` is kept as a pure domain-only boundary policy
until application/Outbox mapping is separately authorized.

## Deferred

```text
Payment
PaymentState
Domain Events
event registration
application handlers
repositories and adapters
```

## Verdict

```text
LOT 3.4: IMPLEMENTED
POLICIES: 14
PROFILES: 12
DOMAIN SERVICES: 4
GLOBAL GENERATION: FORBIDDEN
NEXT: LOT 3.5 EXPLICIT ACTIVATION
```
