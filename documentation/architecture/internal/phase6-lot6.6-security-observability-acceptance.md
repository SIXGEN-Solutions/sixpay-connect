# Phase 6 — Lot 6.6 Security, Observability & Acceptance Gate

## Security

Phase 6 protects the three internal query capabilities with their published
OAuth scopes:

- Payment Query: `payment.read`
- ObservedCustomer Query: `observed-customer.read`
- Payment Audit Query: `payment.audit.read`
- Controlled Audit Export: `payment.audit.read` + `payment.audit.export`

## Observability

Reporting HTTP endpoints are instrumented by a WebMVC interceptor using the
Micrometer timer `sixpay.reporting.audit.http`.

Only low-cardinality tags are emitted:

- normalized operation;
- HTTP outcome class;
- HTTP status.

Payment IDs, audit IDs, export IDs and other customer/business identifiers are
never meter tags.

## OpenAPI ↔ Spring MVC gate

Contract tests compare each authoritative OpenAPI contract with its owning
controller and security boundary. They protect the operations that previously
drifted during implementation, including server-owned `snapshotAt` and export
`202 + Location + Idempotency-Key`.

## Persistence and masking

PostgreSQL/Testcontainers integration tests validate Reporting projection
search and durable export idempotency. A masking gate ensures Reporting schema
and export serializers do not introduce raw NIU/account/credential fields.

## Governance

The implementation gate deliberately does not self-approve contracts.
`approvalStatus: PENDING_APPROVAL`, when still present, must be reconciled by
the designated governance owner rather than changed by implementation code.

## Exit criteria

```text
PHASE6_ARCHITECTURE_GATE = IMPLEMENTED
PHASE6_CONFIGURATION_GATE = IMPLEMENTED
PHASE6_ACCEPTANCE_CATALOG = IMPLEMENTED
PHASE6_CONTRACT_CONFORMANCE_GATE = IMPLEMENTED
PHASE6_SECURITY_GATE = IMPLEMENTED
PHASE6_PERSISTENCE_GATE = IMPLEMENTED
PHASE6_MASKING_GATE = IMPLEMENTED
PHASE6_OBSERVABILITY = IMPLEMENTED
PHASE6_IMPLEMENTATION = COMPLETE_PENDING_GOVERNANCE_AND_GREEN_CI
```
