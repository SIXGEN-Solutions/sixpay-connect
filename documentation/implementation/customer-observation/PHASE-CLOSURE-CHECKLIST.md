# Customer Observation — Phase Closure Checklist

## Contracts

- [ ] Payment projection event schema is versioned.
- [ ] Stable external event type is used.
- [ ] Unknown type/version is rejected.
- [ ] Internal Observed Customer OpenAPI is linted.
- [ ] No Java class name is used as an external contract identifier.

## Persistence

- [ ] Customer migrations validate on an empty PostgreSQL database.
- [ ] Projection replay is idempotent.
- [ ] Stale events are acknowledged without regression.
- [ ] Audit is append-only.
- [ ] Mutation and audit rollback atomically.
- [ ] Query pagination is snapshot-stable and keyset-based.

## Security

- [ ] JWT absence returns 401.
- [ ] Missing scope returns 403.
- [ ] Correct scope grants access.
- [ ] Missing/invalid correlation ID returns 400.
- [ ] API exposes no mutation route.
- [ ] No raw banking data, fingerprint or secret is serialized.

## Observability

- [ ] Projection and query metrics exist.
- [ ] Metric tags are bounded.
- [ ] Logs contain no event payload or customer identity.
- [ ] Duration and lag are recorded.
- [ ] Health indicators expose only aggregate safe details.

## Resilience

- [ ] Optimistic locking is retryable.
- [ ] Deadlock and serialization failures are retryable.
- [ ] Idempotence race reloads the winning projection.
- [ ] Invalid contracts are non-retryable.
- [ ] Backoff is bounded and injected.
- [ ] Exhaustion is explicit.
- [ ] No infinite retry.
- [ ] No `Thread.sleep()` in application/domain.

## Architecture

- [ ] Customer does not depend on Payment.
- [ ] No Amplitude import exists in Customer Observation.
- [ ] Application/domain contain no Spring or JPA.
- [ ] Bootstrap is the inter-module composition point.
- [ ] All phase architecture tests pass.

## Delivery evidence

- [ ] `mvn -pl customer,bootstrap -am clean verify` passes.
- [ ] validation scripts pass.
- [ ] CI is green.
- [ ] documentation is reviewed.
- [ ] release notes identify migrations and operational metrics.
- [ ] branch is ready for review/merge.
