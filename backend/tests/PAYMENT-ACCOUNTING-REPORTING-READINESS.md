# SIXPAY CONNECT — Phase 8.3.3 Payment / Accounting / Reporting

## Status

```text
8.3.3 readiness gate                    IMPLEMENTED
Payment -> Accounting production flow   SEMANTICS DECIDED / PHYSICAL CONTRACT + WIRING STILL BLOCKED BY INT-11
Accounting -> Reporting event flow       NOT DEFINED / MUST NOT BE INVENTED
```

## Authoritative finding

The repository integration architecture classifies INT-11 as:

```text
Producer: Payment
Consumer: Accounting
Mode: Accounting-owned candidate source + scheduled T+1 batch constitution
MVP provider transport: Core Banking Accounting API
Deferred provider transport: CSV/file
Contract: TO_DEFINE for Payment->Accounting production boundary and Core Banking Accounting API
Status: PLANNED / FOUNDATIONS PRESENT
```

A `TO_DEFINE` contract is explicitly a blocking prerequisite for implementation.
Therefore Phase 8.3.3 must not create a production adapter or silently define the
missing event/file contract inside a test.

## Current boundaries

Payment does not depend on Accounting.

Accounting owns `PaymentAccountingCandidateSource`. Its
`AccountingBatchConstitutionService` is conditional on the presence of that
source. No production implementation is currently present.

Reporting currently exposes a JDBC-backed Payment audit read model and export
capabilities. No Accounting event consumer is implemented.

## Test objective

`PaymentAccountingReportingReadinessIT` verifies that:

1. Payment query capability participates in the assembled application.
2. Accounting batch-building capability participates in the assembled application.
3. Reporting audit read/query capability participates in the assembled application.
4. `PaymentAccountingCandidateSource` exists as an explicit Accounting boundary.
5. No bean implements it while INT-11 remains `PLANNED / TO_DEFINE`.
6. `AccountingBatchConstitutionService` therefore remains intentionally absent.
7. Reporting remains independently assembled.

This is a deliberate anti-regression gate. It must evolve into a positive
behavioral cross-module test when INT-11 is approved and implemented.

## Maven

No `pom.xml` change is required. Reuse the existing `assembled-tests` profile.

## Execution

```bash
cd backend

mvn --batch-mode --no-transfer-progress \
  -pl tests -am \
  -Pfull-tests,assembled-tests \
  -Dit.test=PaymentAccountingReportingReadinessIT \
  verify
```

## Anti-regression gates

```bash
mvn --batch-mode --no-transfer-progress -pl payment -am -Pfull-tests -DskipITs=false verify
mvn --batch-mode --no-transfer-progress -pl accounting -am -Pfull-tests -DskipITs=false verify
mvn --batch-mode --no-transfer-progress -pl reporting -am -Pfull-tests -DskipITs=false verify
mvn --batch-mode --no-transfer-progress -pl tests -am -Pfull-tests -Dit.test=GoldenModuleE2EIT verify
```

## Exit rule

The real Payment / Accounting financial flow cannot be declared complete until
INT-11 has an approved/versioned contract, implementation, idempotency and
reconciliation semantics, and positive cross-module behavioral tests.
