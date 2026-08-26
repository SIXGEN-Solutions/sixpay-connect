# Customer Observation — E2E Acceptance Matrix

| ID | Scenario | Expected result | Primary evidence |
|---|---|---|---|
| E2E-01 | Payment projection event persisted in Outbox | Stable contract name and schema version stored | Payment serialization integration test |
| E2E-02 | PENDING Outbox event dispatched to Customer | Customer projection APPLIED and Outbox PUBLISHED | Bootstrap dispatcher integration test |
| E2E-03 | Same source event replayed | Customer returns REPLAYED; Outbox PUBLISHED | Projection/JPA integration tests |
| E2E-04 | Older event received after newer event | IGNORED_STALE; current projection preserved | Projection/JPA integration tests |
| E2E-05 | Temporary database failure | Retry with bounded backoff | Resilience policy and transaction tests |
| E2E-06 | Retry exhausted | Explicit exhausted failure and safe metric | Resilience tests |
| E2E-07 | Invalid event contract | Dead-letter/non-retryable result | Serialization/dispatcher tests |
| E2E-08 | Concurrent duplicate event | One APPLIED, one REPLAYED | PostgreSQL concurrency test |
| E2E-09 | Query search with valid JWT/scope/correlation | HTTP 200, stable snapshot and opaque cursor | MVC/API contract tests |
| E2E-10 | Query without JWT | HTTP 401 | Security API test |
| E2E-11 | Query without required scope | HTTP 403 | Security API test |
| E2E-12 | Query without/invalid correlation header | HTTP 400 | Controller/API test |
| E2E-13 | Unknown projection | HTTP 404 | Query API test |
| E2E-14 | Temporary query repository failure | HTTP 503 | Query exception-handler test |
| E2E-15 | Projection mutation succeeds | Append-only audit persisted atomically | Audit transaction integration test |
| E2E-16 | Projection mutation rolls back | Projection and audit both rolled back | Audit transaction integration test |
| E2E-17 | Read audit unavailable | Read succeeds according to fail-open policy; failure metric increments | Query audit test |
| E2E-18 | Health endpoints evaluated | UP, DEGRADED or DOWN with safe aggregate details | Health indicator tests |
| E2E-19 | Sensitive-field scan | No raw NIU/account/token/payload in logs, metrics or API | Architecture and serialization tests |
| E2E-20 | Restart and recovery | Persisted projection, audit and Outbox events remain readable | Restart integration tests |

## Mandatory acceptance rule

The phase cannot be closed with:

- a skipped failing test;
- an unresolved Flyway validation error;
- an unlinted internal OpenAPI contract;
- a direct Customer → Payment dependency;
- a secret committed in YAML;
- a sensitive value used as a metric tag;
- an infinite retry or `Thread.sleep()` in application/domain.
