# SIXPAY CONNECT — Notification Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.6 — Notification
8.2.9 remediation — Operational Notification closure
```

## 1. Current implementation scope

Notification contains two explicit functional areas:

```text
Partner decision notification
Operational notification
```

Both are validated independently and remain inside the owning Notification
module.

---

## 2. Partner decision notification

Existing focused evidence covers:

```text
PartnerDecisionNotificationServiceTest
RetryNotificationDeliveriesServiceTest
JacksonPartnerStatusChangedEventDecoderTest
NotificationDeliveryPersistenceIT
NotificationApplicationAutoConfigurationTest
```

Classification:

```text
Application     COVERED
Infrastructure  COVERED
```

---

## 3. Operational Notification — Domain

The Operational subsystem owns a real domain layer.

Existing focused tests include:

```text
NotificationDeliveryLifecycleTest
OperationalNotificationRetryPolicyTest
OperationalNotificationRoutingPolicyTest
NotificationDeduplicationKeyFactoryTest
```

They prove:

```text
allowed lifecycle transitions
terminal states
retryable redispatch/dead-letter transitions
bounded exponential backoff
retry exhaustion
routing to channel/template
stable functional deduplication
recipient-sensitive deduplication
```

The domain model also enforces invariants directly in constructors/records,
including non-negative attempt counters and required state data.

Classification:

```text
DOMAIN = COVERED
```

---

## 4. Operational Notification — Application

Existing focused tests include:

```text
OperationalNotificationPlanningServiceTest
OperationalNotificationOrchestrationServiceTest
OperationalNotificationDeliveryServiceTest
OperationalNotificationOperationsServiceTest
OperationalNotificationRetentionServiceTest
```

Evidence includes:

```text
trigger -> notification planning
admin recipient resolution
template selection
no sensitive account/NIU variables in generated intent
functional idempotence
source transaction isolation on notification persistence failure
retryable failure scheduling
permanent failure terminal handling
dead-letter after retry exhaustion
provider success completion
manual dead-letter replay
replay audit creation
notification identity/deduplication preservation
status output without raw email address
separate delivered/failed retention windows
purge telemetry
```

Classification:

```text
APPLICATION = COVERED
```

---

## 5. Operational Notification — API

The module POM does not establish a module-owned Spring MVC business API for
Operational Notification.

The operational use cases are exposed through application ports, scheduling,
metrics and infrastructure adapters rather than a Notification REST controller.

Phase 8.2 does not invent an HTTP API solely for symmetry with `partner`.

Classification:

```text
API = N/A
```

---

## 6. Operational Notification — Infrastructure

Existing focused infrastructure evidence includes:

```text
OperationalEmailTemplateRendererTest
OperationalSmtpNotificationDeliveryGatewayTest
```

They prove:

```text
versioned template rendering
template-variable contract validation
SMTP accepted result
retryable SMTP send failure
permanent SMTP authentication failure
masked recipient logging
no payment-reference leakage in operational logs
```

### PostgreSQL persistence evidence added by 8.2.9 remediation

Added:

```text
OperationalNotificationPersistenceIT
```

This test runs against PostgreSQL Testcontainers and validates the actual
`OperationalNotificationPersistenceAdapter`.

Covered persistence behaviors:

```text
functional idempotence through deduplication_key
insert-if-absent semantics
template-variable serialization round-trip
find by id
find by deduplication key
due ordering
batch limit
atomic claim behavior
attempt-count increment
cycle-attempt-count increment
duplicate claim rejection
attempt persistence and ordering
dead-letter replay
replay audit persistence
notification identity preservation
deduplication-key preservation
operational status counts
due count
oldest due timestamp
```

This is the missing database-level evidence required by the golden checklist.

Classification:

```text
INFRASTRUCTURE = COVERED
```

---

## 7. Auto-configuration ownership

Operational Notification is composed through:

```text
OperationalNotificationPersistenceAutoConfiguration
OperationalNotificationApplicationAutoConfiguration
OperationalNotificationEmailAutoConfiguration
OperationalNotificationOperationsAutoConfiguration
OperationalNotificationRetryAutoConfiguration
```

The new persistence IT intentionally imports only the persistence
auto-configuration and excludes unrelated Notification auto-configurations.

This preserves the golden rule:

```text
one test = one responsibility
```

No fake sender, scheduler, operations bean or legacy Partner-notification
component is introduced into the persistence test context.

---

## 8. Final Notification classification

```text
Partner notification
  Application     COVERED
  Infrastructure  COVERED

Operational notification
  Domain          COVERED
  Application     COVERED
  API             N/A
  Infrastructure  COVERED
```

Overall:

```text
NOTIFICATION = COVERED FOR CURRENT IMPLEMENTATION
```

The previous open Operational Notification verification state is closed.

---

## 9. Maven infrastructure

The module already contains the required test stack:

```text
Spring Boot Test
Flyway test support
PostgreSQL test driver
Testcontainers JUnit Jupiter
Testcontainers PostgreSQL
```

No `pom.xml` change is required.

---

## 10. Validation

From `backend/`:

```bash
mvn -pl notification \
    -Dtest=OperationalNotificationPersistenceIT \
    test
```

Then:

```bash
mvn -pl notification -am test
```

Full integration validation:

```bash
mvn -pl notification -am \
    -Pfull-tests clean verify
```

Finally re-run the 8.2.9 gate:

```bash
mvn -pl tests \
    -Dtest=BackendGoldenCoverageGateTest \
    test
```

---

## 11. Exit decision

The Notification blocker in 8.2.9 is resolved when:

```text
OperationalNotificationPersistenceIT = GREEN
notification module tests = GREEN
NOTIFICATION-TEST-COVERAGE.md contains no open verification marker
```
