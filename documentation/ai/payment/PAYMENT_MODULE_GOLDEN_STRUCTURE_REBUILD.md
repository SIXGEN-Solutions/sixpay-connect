# Payment Module — Golden Module Structure Rebuild

## Source rule

`partner` is the Golden Module. Payment must use:

```text
api/
  request/
  response/
  security/
application/
configuration/
domain/
events/
infrastructure/
```

## Removed

The following invented structures are forbidden:

```text
payment.infrastructure.web
payment.infrastructure.web.command
payment.infrastructure.web.dto
```

The earlier Lot 3.16 command API delivery is invalidated and removed.

## Preserved

No requirement from Lots 3.1–3.15 is removed:

- aggregate and domain events;
- persistence and Flyway;
- audit;
- outbox;
- idempotency and replay;
- application commands, queries, views and ports;
- focused orchestration services;
- banking adapters;
- TFJ reconciliation separation;
- query API behavior;
- security policies and Partner isolation;
- observability;
- integration, concurrency and final validation tests.

Only package ownership of the HTTP adapter is changed.

## Golden mapping

| Partner | Payment |
|---|---|
| `partner.api.PartnerController` | `payment.api.PaymentQueryController` and future `PaymentController` |
| `partner.api.request.*` | `payment.api.request.*` |
| `partner.api.response.*` | `payment.api.response.*` |
| `partner.api.PartnerApiExceptionHandler` | `payment.api.PaymentApiExceptionHandler` |
| `partner.api.security.*` | REST-facing Payment access policy only |
| `partner.application.*` | `payment.application.*` |
| `partner.configuration.PartnerModuleConfiguration` | `payment.configuration.PaymentModuleConfiguration` |
| `partner.infrastructure.*` | `payment.infrastructure.*` |

## Important command API decision

The invalid Lot 3.16 implementation is removed. A new command endpoint must not
be reintroduced until its application orchestration and Partner subscription
authorization contract are validated against the current branch.

The request/response records are retained in the correct Golden Module package
as preparation, but no incomplete production controller is activated.
