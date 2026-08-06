# Integration Error Taxonomy

## 1. Purpose

This taxonomy provides a common classification for synchronous clients, callbacks, Kafka consumers, outbox relays, SFTP transfers and internal module adapters.

Business outcomes must never be disguised as technical failures, and technical failures must never be converted into business declines.

## 2. Canonical classes

| Code | Class | Meaning | Retry default | HTTP mapping where applicable | Operational action |
|---|---|---|---|---|---|
| `BUSINESS_REJECTED` | business | Valid request rejected by business rule, e.g. insufficient funds or opposed account | no | 200/4xx according to published contract | record outcome; no incident unless threshold exceeded |
| `VALIDATION_FAILED` | caller/data | Request violates schema or semantic preconditions | no | 400/422 | return field-safe details; fix producer |
| `AUTHENTICATION_FAILED` | security | Missing/invalid token, certificate or signature | no automatic retry | 401 | security alert if unexpected |
| `AUTHORIZATION_FAILED` | security | Identity is valid but not permitted | no | 403 | audit denied access |
| `NOT_FOUND` | business/query | Requested resource or provider record is absent | no, unless eventual consistency is documented | 404 | normal or reconciliation decision |
| `CONFLICT` | state/idempotency | State conflict, duplicate with different payload, invalid transition | no | 409 | caller or operator resolution |
| `RATE_LIMITED` | dependency | Provider throttled the request | yes, honor `Retry-After`, bounded | 429 | metric and capacity review |
| `TIMEOUT_BEFORE_SEND` | transport | Request was not transmitted | usually yes if operation is idempotent | 504/503 | retry with backoff |
| `TIMEOUT_OUTCOME_UNKNOWN` | financial safety | Request may have reached provider; outcome unknown | never blind-retry | 202/503 or internal recoverable state | lookup/reconcile before retry or reversal |
| `DEPENDENCY_UNAVAILABLE` | dependency | Connection failure, maintenance or eligible 5xx | bounded | 503 | retry/circuit protection/alert |
| `PROTOCOL_ERROR` | provider contract | Unsupported status, headers or protocol behavior | no until classified | 502 | quarantine evidence and alert |
| `INVALID_RESPONSE` | provider data | Empty, malformed or semantically invalid response | no blind retry | 502 | quarantine, alert provider owner |
| `INTEGRITY_FAILED` | security/data | Signature, checksum, schema fingerprint or file total invalid | no | 400/502 | quarantine and security/operations alert |
| `DUPLICATE` | delivery | Same event/file/request already processed | acknowledge safely | 200/204 | metric only |
| `ORDERING_VIOLATION` | event/state | Event arrives before required predecessor | delayed retry or quarantine | n/a | inspect partition/key and replay |
| `RETRY_EXHAUSTED` | resilience | Retry budget consumed | no further automatic retry | 503 or DLQ | alert and controlled replay |
| `CONFIGURATION_ERROR` | deployment | Missing URL, credential, certificate, topic or mapping | no | 500/503 | fail health/readiness and remediate |
| `INTERNAL_ERROR` | SIXPAY defect | Unexpected code or persistence failure | bounded only when transaction safety is known | 500 | incident and root-cause analysis |

## 3. Business versus technical examples

| Scenario | Classification |
|---|---|
| account exists but is opposed | `BUSINESS_REJECTED` |
| available balance is below amount | `BUSINESS_REJECTED` |
| unknown customer NIU | `NOT_FOUND` or business rejection according to contract |
| OAuth2 token endpoint rejects client | `AUTHENTICATION_FAILED` |
| Amplitude returns HTML instead of JSON | `INVALID_RESPONSE` |
| TLS trust chain fails | `AUTHENTICATION_FAILED` or `CONFIGURATION_ERROR` |
| posting request times out after bytes were sent | `TIMEOUT_OUTCOME_UNKNOWN` |
| Kafka redelivers same event | `DUPLICATE` |
| TFJ checksum differs | `INTEGRITY_FAILED` |
| callback endpoint returns 429 | `RATE_LIMITED` |

## 4. Retry decision table

| Operation | Safe automatic retry? | Preconditions |
|---|---|---|
| read-only lookup | yes | bounded attempts, timeout budget, no rate-limit violation |
| customer/account verification | usually yes | provider confirms read-only semantics |
| balance/funds inquiry | usually yes | operation has no reservation side effect |
| funds reservation/control | only if provider idempotency is certified | stable idempotency key |
| posting | no blind retry | first resolve by idempotency key/bank reference |
| reversal | no blind retry | stable reversal key and outcome lookup |
| callback | yes | same immutable callback event ID and signature semantics |
| Kafka consumer | yes | idempotent consumer and bounded retry/DLQ |
| SFTP upload | yes with safeguards | temporary file, checksum, atomic rename and duplicate detection |

## 5. Error envelope requirements

Public and internal HTTP error responses SHALL contain only approved fields:

- stable error code;
- safe human-readable message;
- correlation ID;
- timestamp;
- field violations where appropriate;
- retryable indicator only when contractually reliable.

They SHALL NOT expose:

- stack traces;
- SQL or class names;
- tokens, API keys or certificate details;
- full account numbers or personal identifiers;
- provider credentials;
- raw provider body unless explicitly sanitized.

## 6. Provider error mapping

Each provider adapter must own a mapping table:

| Provider signal | Canonical class | Business effect | Retry | Evidence retained |
|---|---|---|---|---|
| provider business code | business/not-found/conflict | domain outcome | no | sanitized code and reference |
| 400 | validation/protocol | integration failure unless contract says business | no | status and sanitized body fingerprint |
| 401/403 | authentication/authorization | technical failure | no | status, endpoint ID, correlation |
| 404 | not-found or protocol | contract-specific | usually no | status and lookup key fingerprint |
| 409 | conflict/idempotency | resolve state | no blind retry | provider reference |
| 429 | rate-limited | recoverable | bounded, `Retry-After` | attempt count |
| 5xx | unavailable/protocol | recoverable unless malformed | bounded | status and correlation |
| read timeout | timeout class | unknown if side-effecting | operation-specific | send state and idempotency key |
| parse/semantic error | invalid response | technical | no blind retry | schema/error fingerprint |

## 7. Asynchronous terminal states

Recommended transport-neutral states:

- `PENDING`;
- `CLAIMED`;
- `DELIVERED`;
- `RETRY_SCHEDULED`;
- `QUARANTINED`;
- `DEAD_LETTERED`;
- `REPLAY_REQUESTED`;
- `REPLAYED`.

Every terminal failure record must include:

- event/message/file ID;
- integration ID;
- canonical error class;
- sanitized cause;
- attempt count;
- first/last failure timestamps;
- correlation ID;
- replay eligibility;
- operator/runbook reference.

## 8. Observability rules

Metrics should include low-cardinality tags only:

- integration ID;
- operation;
- provider;
- result/error class;
- HTTP status family;
- retry attempt bucket.

Never tag metrics with account number, NIU, payment reference, callback URL, event ID or correlation ID.

Logs may carry correlation ID and masked references, but must follow data-classification rules.

## 9. Health and readiness

- missing mandatory credentials/certificates/configuration: readiness `DOWN`;
- provider temporarily unavailable: dependency health `DOWN` or `DEGRADED` without leaking secrets;
- outbox backlog above threshold: `DEGRADED`;
- oldest undelivered message above SLA: `DOWN` or alert according to criticality;
- DLQ/quarantine growth: alert;
- query APIs may remain live while non-critical notification integration is degraded, but financial posting readiness must block unsafe processing.
