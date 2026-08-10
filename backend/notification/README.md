# Notification

The Notification module contains two functional areas:

```text
Partner decision notification
Operational notification
```

## Partner decision notification

The Partner-decision flow remains decoupled from the `partner` Java/Maven
module and consumes integration events through Notification-owned adapters.

## Operational notification

Operational Notification owns:

```text
domain models and policies
planning/orchestration
delivery lifecycle and retry
operations/replay/retention
PostgreSQL persistence
SMTP delivery
metrics/scheduling composition
```

## Phase 8 golden coverage

Current classification:

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

The PostgreSQL evidence for the Operational persistence adapter is provided by:

```text
OperationalNotificationPersistenceIT
```

Detailed evidence is maintained in:

```text
NOTIFICATION-TEST-COVERAGE.md
```

## Validation

```bash
mvn -pl notification \
    -Dtest=OperationalNotificationPersistenceIT \
    test

mvn -pl notification -am test

mvn -pl notification -am \
    -Pfull-tests clean verify
```
