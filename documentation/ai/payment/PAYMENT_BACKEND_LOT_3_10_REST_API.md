# SIXPAY CONNECT — Phase 3 / Lot 3.10

## REST API

Authoritative contract: `documentation/contracts/internal/payment-query-api-v1.yaml`.

Generated endpoints:

```text
GET /internal/api/v1/payments
GET /internal/api/v1/payments/{paymentId}
```

The API is read-only, requires `payment.read`, echoes `X-Correlation-ID`, returns masked projections, and never loads the Payment aggregate.

The controller is conditional on a compliant `PaymentProjectionQueryUseCase` implementation. No such read-model implementation exists on the authoritative branch, so the controller remains inactive rather than returning fabricated data.

The contract PaymentStatus enum is not aligned with the current domain lifecycle. No lossy mapping is introduced. The contract must be updated or an approved compatibility mapping must be provided before activation.
