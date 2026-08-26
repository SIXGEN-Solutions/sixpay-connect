# Phase 6 — Closure Checklist

- [ ] `payment-query-api-v1.yaml` matches Spring MVC implementation.
- [ ] `observed-customer-query-api-v1.yaml` matches Spring MVC implementation.
- [ ] `payment-audit-query-api-v1.yaml` matches Spring MVC implementation.
- [ ] Payment Query security tests pass.
- [ ] ObservedCustomer Query security tests pass.
- [ ] Payment Audit read/export security tests pass.
- [ ] Reporting persistence IT passes on PostgreSQL 17.
- [ ] Audit export idempotency IT passes.
- [ ] Audit masking gate passes.
- [ ] Reporting Micrometer HTTP observations are registered.
- [ ] HMAC cursor key is supplied through runtime secret management.
- [ ] Audit export storage/retrieval configuration is supplied by deployment.
- [ ] Flyway validation passes from bootstrap.
- [ ] `mvn -pl payment,customer,reporting,bootstrap -am test` passes.
- [ ] Contract approval metadata is reconciled by the contract governance owner.

## Production-readiness note

This checklist does not silently convert `PENDING_APPROVAL` contract metadata.
Approval remains an explicit governance action by the contract owner.
