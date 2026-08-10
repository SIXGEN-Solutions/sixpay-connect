# SIXPAY CONNECT — Final Backend Golden Coverage Gate

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.9 — Final backend golden coverage gate
```

## 1. Purpose

8.2.9 is not another functional test suite.

It is the closure gate for the module-local backend golden coverage work
performed in 8.2.1 through 8.2.8.

The gate must answer one question:

```text
Can Backend Golden Test Coverage be declared complete
for the authoritative implementation branch?
```

The answer is based on repository evidence, not on test-count inflation.

The `partner` module remains the golden implementation and test-structure
reference.

---

## 2. Source of truth

Authoritative branch:

```text
feat/sixpay-test-validate-pilote
```

Mandatory entry point:

```text
ENGINEERING_CONTEXT.md
```

Repository principle:

```text
Consistency takes precedence over creativity.
```

---

## 3. Existing CI execution

Backend CI already executes:

```text
quality
  mvn clean verify

integration
  mvn clean verify -Pfull-tests,coverage
```

and publishes:

```text
Surefire reports
Failsafe reports
JaCoCo reports
Testcontainers diagnostics on failure
```

8.2.9 therefore does not duplicate the CI test runner.

Its responsibility is coverage-state conformance.

---

## 4. Canonical module matrix

The final gate expects coverage evidence in the owning module:

```text
backend/customer/CUSTOMER-TEST-COVERAGE.md
backend/subscription/SUBSCRIPTION-TEST-COVERAGE.md
backend/payment/PAYMENT-TEST-COVERAGE.md
backend/accounting/ACCOUNTING-TEST-COVERAGE.md
backend/reporting/REPORTING-TEST-COVERAGE.md
backend/notification/NOTIFICATION-TEST-COVERAGE.md
backend/administration/ADMINISTRATION-TEST-COVERAGE.md
backend/security/SECURITY-TEST-COVERAGE.md
```

A business/platform module MUST NOT be nested under `partner`.

In particular:

```text
backend/security/
```

is canonical.

This is forbidden:

```text
backend/partner/security/
```

The golden module is a reference, not a container for sibling modules.

---

## 5. Gate semantics

The executable gate fails when a canonical coverage document contains an open
marker such as:

```text
PARTIAL
TO_VERIFY
UNVERIFIED
TODO
TBD
```

A module with no production implementation is allowed only when its status is
explicit and honest, for example:

```text
DEFERRED
NOT_IMPLEMENTED
```

This distinction is important:

```text
implemented but incompletely verified
    -> BLOCKING

not implemented and explicitly classified
    -> ACCEPTED AS DEFERRED
```

---

## 6. Current authoritative-branch assessment

At the time 8.2.9 is introduced, the branch is NOT ready for a PASS.

### Customer

Authoritative evidence still says:

```text
Infrastructure — Observation persistence/query = PARTIAL
CUSTOMER = PARTIAL
```

Gate:

```text
BLOCKED
```

Required closure:

```text
positively verify existing PostgreSQL behavioral coverage
or add the smallest missing focused persistence/query IT
then update CUSTOMER-TEST-COVERAGE.md
```

### Subscription

The implementation is explicitly a placeholder and the coverage document says:

```text
DEFERRED — NO IMPLEMENTED CAPABILITY TO TEST
```

Gate:

```text
ACCEPTED AS DEFERRED
```

No fictional Subscription tests are required.

### Payment

Authoritative evidence still says:

```text
APPLICATION = PARTIAL
INFRASTRUCTURE = PARTIAL
PAYMENT = PARTIAL
```

Gate:

```text
BLOCKED
```

Required closure:

```text
finish application/security-policy evidence
positively verify persistence/query/integration evidence
update PAYMENT-TEST-COVERAGE.md
```

### Accounting

Authoritative evidence says:

```text
DOMAIN = COVERED
APPLICATION = COVERED
API = N/A
INFRASTRUCTURE = COVERED
ACCOUNTING = COVERED
```

Gate:

```text
PASS
```

### Reporting

Authoritative evidence says:

```text
DOMAIN = COVERED
APPLICATION = COVERED
API = COVERED
INFRASTRUCTURE = COVERED
REPORTING = COVERED
```

Gate:

```text
PASS
```

### Notification

Authoritative evidence still says:

```text
Operational notification
  Domain          TO_VERIFY
  Application     TO_VERIFY
  Infrastructure  TO_VERIFY

API               TO_VERIFY / N/A
```

and explicitly says 8.2.6 must not yet be CLOSED.

Gate:

```text
BLOCKED
```

Required closure:

```text
audit Operational Notification
close its Domain/Application/Infrastructure coverage
resolve API classification
update NOTIFICATION-TEST-COVERAGE.md
```

### Administration

The module is explicitly classified as an unimplemented module shell:

```text
NOT_IMPLEMENTED
```

Gate:

```text
ACCEPTED AS NOT IMPLEMENTED
```

This does not certify Administration functionality.

### Security

The 8.2.8 content classifies current Security responsibilities as covered.

However, the authoritative branch currently contains the new coverage assets
under:

```text
backend/partner/security/
```

instead of:

```text
backend/security/
```

Gate:

```text
BLOCKED BY STRUCTURAL DRIFT
```

Required closure:

```text
move SECURITY-TEST-COVERAGE.md
move README update
move SixpaySecurityAutoConfigurationTest.java

from backend/partner/security/
to backend/security/

remove backend/partner/security/
```

---

## 7. Current gate result

```text
FINAL BACKEND GOLDEN COVERAGE GATE = FAIL
```

Blocking items:

```text
GATE-001 Customer remains PARTIAL
GATE-002 Payment remains PARTIAL
GATE-003 Notification Operational remains TO_VERIFY
GATE-004 Security Phase 8.2.8 assets are under the wrong module path
```

Accepted exceptions:

```text
Subscription = DEFERRED because capability is not implemented
Administration = NOT_IMPLEMENTED module shell
```

Passing modules:

```text
Accounting
Reporting
```

Security content is substantively covered but structurally misplaced.

---

## 8. Executable gate

8.2.9 adds:

```text
backend/tests/src/test/java/com/sixpay/tests/gate/
    BackendGoldenCoverageGateTest.java
```

The test verifies:

```text
all required coverage documents exist at canonical paths
no coverage document contains blocking status markers
Security is not nested under partner
deferred/unimplemented modules are explicitly classified
```

It does not replace:

```text
module unit tests
module WebMvc tests
module PostgreSQL ITs
cross-module Phase 8.3 tests
```

---

## 9. Why a red gate is correct

The purpose of a final gate is to prevent premature closure.

A test that is forced green while authoritative evidence still says
`PARTIAL` or `TO_VERIFY` would make Phase 8 documentation less trustworthy.

Therefore the initial 8.2.9 gate is expected to fail until GATE-001 through
GATE-004 are resolved.

---

## 10. Local validation

From `backend/`:

```bash
mvn -pl tests \
    -Dtest=BackendGoldenCoverageGateTest \
    test
```

Expected initial result:

```text
FAIL
```

After blockers are resolved:

```text
PASS
```

Complete backend validation:

```bash
mvn --batch-mode --no-transfer-progress \
    clean verify

mvn --batch-mode --no-transfer-progress \
    clean verify -Pfull-tests,coverage
```

---

## 11. CI integration

No workflow change is required.

`backend/tests` is already part of the Maven reactor, so the gate participates
in the normal:

```text
mvn clean verify
```

quality job.

The integration/coverage job continues to validate PostgreSQL/Testcontainers
and JaCoCo evidence independently.

---

## 12. 8.2 exit rule

Lot 8.2 may be declared CLOSED only when:

```text
BackendGoldenCoverageGateTest = GREEN

AND

mvn clean verify = GREEN

AND

mvn clean verify -Pfull-tests,coverage = GREEN
```

At that point, the authoritative status may become:

```text
PHASE 8 / LOT 8.2
BACKEND GOLDEN TEST COVERAGE = PASS
```

Until then:

```text
LOT 8.2 = OPEN
```

---

## 13. Next work order

Resolve blockers in this order:

```text
1. GATE-004 Security structural relocation
2. GATE-003 Notification Operational audit
3. GATE-001 Customer persistence/query closure
4. GATE-002 Payment application/infrastructure closure
5. Run BackendGoldenCoverageGateTest
6. Run full backend quality + integration/coverage commands
7. Mark Lot 8.2 CLOSED only if all are green
```
