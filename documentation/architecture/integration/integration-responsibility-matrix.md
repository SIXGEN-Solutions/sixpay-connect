# Integration Responsibility Matrix

## 1. Status legend

- `IMPLEMENTED`: production-oriented implementation exists.
- `FOUNDATION`: ports/adapters exist but the real provider transport or contract is incomplete.
- `PLANNED`: no complete consumer/provider implementation was found.
- `TO_DEFINE`: a versioned contract is required before implementation.
- `INTERNAL_JAVA`: in-process contract owned by the receiving module.

## 2. Mandatory responsibility matrix

| ID | Producer | Consumer | Protocol / mode | Contract | Contract owner | Security policy | Error policy | Test mode | Status | Next lot |
|---|---|---|---|---|---|---|---|---|---|---|
| INT-01 | TresorPay | Payment | HTTPS REST, synchronous | `external/payment-command-api-v1.yaml` | Payment | mTLS plus approved bearer/API credential model; idempotency key; correlation | RFC 7807-style public errors; business rejection non-retryable; transient 5xx/timeout retryable under idempotency | OpenAPI validation, controller tests, security tests, TresorPay stub, E2E | IMPLEMENTED | 5.2 |
| INT-02 | Payment | TresorPay callback endpoint | HTTPS callback, async via outbox | callback contract to publish/confirm | Payment | HTTPS, endpoint allow-list, detached JWS, key rotation, no sensitive payload | retry transient transport failures; quarantine permanent 4xx/signature/configuration failures | signer tests, callback stub, retry/replay, E2E | FOUNDATION | 5.2 |
| INT-03 | Payment | Customer Verification | in-process Java, synchronous | `BankingCustomerVerificationPort`-backed internal adapter / receiving use case | Customer | authenticated internal execution context; no network credential | business FAIL returned normally; technical exceptions mapped to recoverable/non-recoverable Payment outcomes | unit, architecture and intermodule integration tests | IMPLEMENTED | maintain |
| INT-04 | Customer Verification | Amplitude | HTTPS REST, synchronous | provider contract to validate | Core Banking provider; Customer owns mapping | OAuth2 client credentials, mTLS SSL bundle, correlation/request IDs | classify auth, timeout, unavailable, protocol and invalid response; bounded retry only for transient classes | mapper/client tests, OAuth2/mTLS stub, sandbox certification | IMPLEMENTED / UNCONFIRMED CONTRACT | 5.3 |
| INT-05 | Payment | Core Banking verification | provider call, synchronous | `TO_DEFINE` | Core Banking provider; Payment owns mapping | OAuth2/mTLS expected; final provider policy required | business negative evidence returned; technical failure classified | contract test, stub, sandbox, E2E | FOUNDATION | 5.4 |
| INT-06 | Payment | Core Banking funds control | provider call, synchronous | `TO_DEFINE` | Core Banking provider; Payment owns mapping | OAuth2/mTLS; financial idempotency rules required | insufficient funds/opposition are business outcomes; timeout/5xx technical | contract test, concurrency/idempotency, sandbox | FOUNDATION | 5.4 |
| INT-07 | Payment | Core Banking posting | provider call, synchronous with unknown-outcome handling | `TO_DEFINE` | Core Banking provider; Payment owns mapping | OAuth2/mTLS; mandatory idempotency key | never blind-retry unknown posting; resolve by lookup before compensation | stub, unknown-outcome, idempotency, sandbox, E2E | FOUNDATION | 5.4 |
| INT-08 | Payment | Core Banking lookup | provider query, synchronous | `TO_DEFINE` | Core Banking provider | OAuth2/mTLS | not-found distinct from unavailable; bounded retry on transient query failure | lookup fixtures and reconciliation E2E | FOUNDATION | 5.4 |
| INT-09 | Payment | Core Banking reversal | provider call, synchronous | `TO_DEFINE` | Core Banking provider; Payment owns mapping | OAuth2/mTLS; reversal authorization and idempotency | duplicate reversal must be safe; ambiguous result resolved by lookup/manual workflow | contract, idempotency, compensation and sandbox tests | FOUNDATION | 5.4 |
| INT-10 | Payment outbox | Observed Customer projection | transactional outbox + scheduled internal dispatch | code-level canonical mapping; distributed schema `TO_DEFINE` if externalized | Customer owns command; Payment owns emitted facts | trusted in-process boundary; encrypt sensitive persisted projection data | retry transient projection failures; classify permanent data errors; replay with deduplication | atomicity, dispatcher, replay, projection and E2E tests | IMPLEMENTED | 5.5 |
| INT-11 | Payment | Accounting | async event and/or TFJ/SFTP | `TO_DEFINE` | Accounting; external format owned by Core Banking | Kafka ACL or SFTP key/mTLS as selected; file integrity controls | retry transport; quarantine rejected files/events; reconcile duplicates | event/file contract, SFTP container, reconciliation and replay | PLANNED | 5.6 |
| INT-12 | Payment | Notification | async integration event | `TO_DEFINE` | Notification | least-privilege provider credentials; consent and PII controls | notification failure must not roll back Payment; retry then DLQ | consumer contract, provider stub, DLQ/replay | PLANNED | 5.7 |
| INT-13 | Internal operator/client | Payment Query API | HTTPS REST, synchronous | `internal/payment-query-api-v1.yaml` | Payment | OAuth2/JWT, role and partner isolation, object-level authorization | 400/401/403/404/429/5xx taxonomy; no sensitive leakage | contract, controller, security and projection IT | IMPLEMENTED | maintain |
| INT-14 | Internal operator/client | Observed Customer Query API | HTTPS REST, synchronous | `internal/observed-customer-query-api-v1.yaml` | Customer | OAuth2/JWT, masking, rate limit, audit | invalid cursor 400; forbidden 403; absent 404; unavailable 503 | contract, security, audit, masking, cursor and persistence IT | IMPLEMENTED | maintain |
| INT-15 | Internal auditor | Payment Audit Query API | HTTPS REST, synchronous | `internal/payment-audit-query-api-v1.yaml` | Payment | privileged audit scope, object access, immutable audit trail | reject unauthorized access; distinguish absence from repository failure | contract and audit persistence/API tests | VERIFY ALIGNMENT | 5.8 |

## 3. RACI-style ownership

| Concern | Accountable | Responsible contributors | Consulted | Informed |
|---|---|---|---|---|
| TresorPay API and callback | Payment | Partner, Security, Integration | TresorPay | Operations |
| Customer Verification internal contract | Customer | Payment, Bootstrap | Security | Operations |
| Amplitude customer verification | Customer | Integration, Security | Core Banking provider | Payment |
| Amplitude financial operations | Payment | Integration, Security | Core Banking provider, Accounting | Operations |
| Observed Customer projection | Customer | Payment, Bootstrap | Security, Data | Operations |
| Payment distributed events | Payment | Integration | Customer, Accounting, Notification | Operations |
| Accounting/TFJ/SFTP | Accounting | Payment, Integration | Core Banking provider, Operations | Audit |
| Notification | Notification | Integration | Payment, Security/Privacy | Operations |
| Shared integration foundation | Integration/platform | all producer/consumer teams | Security, Operations | Architecture |

## 4. Contract change rule

A breaking change requires:

1. a new contract version;
2. compatibility impact assessment;
3. producer and consumer approval;
4. migration and rollback plan;
5. contract tests for both versions during transition;
6. update of this matrix and both flow documents.
