# Phase 8.3.7 — Final Cross-module Gate

## Purpose

This gate closes lot 8.3 without changing any business module.

It keeps the existing focused harness isolated and executes the assembled
application tests through a dedicated Maven profile that cannot silently skip
integration tests.

## Coverage mapping

| Sub-lot | Existing gate |
|---|---|
| 8.3.1 Assembled Application Context | `AssembledApplicationContextIT` |
| 8.3.2 Payment / Customer Observation | Assertions in `PilotCriticalFlowMatrixIT` (`ObservedCustomerProjectionPort` and `PaymentObservedCustomerProjectionService`) |
| 8.3.3 Payment / Accounting / Reporting | `PaymentAccountingReportingReadinessIT` |
| 8.3.4 Payment / Notification | `PaymentNotificationReadinessIT` |
| 8.3.5 Hybrid Security Assembly | `HybridSecurityAssemblyIT` |
| 8.3.6 Pilot Critical Flow Matrix | `PilotCriticalFlowMatrixIT` |
| 8.3.7 Final Cross-module Gate | `cross-module-gate` Maven profile + `FinalCrossModuleGateTest` |

No duplicate 8.3.2 test is introduced because the final matrix already checks
the approved Payment -> Customer Observation assembly boundaries.

## Canonical command

Run from `backend/`:

```bash
mvn -pl tests -am -Pcross-module-gate verify
```

The profile:

- sets `skipITs=false`;
- sets `failIfNoTests=true`;
- adds `bootstrap` only in the gate profile;
- activates `sixpay.assembled.tests=true`;
- executes only `com.sixpay.tests.assembled` integration tests.

This isolation is intentional. It prevents the full assembled classpath from
being injected into focused tests such as `GoldenModuleE2EIT`.

## Exit criteria

The gate is green only when:

1. all required assembled test assets exist;
2. Payment / Customer Observation remains represented in the final matrix;
3. the assembled application tests actually execute;
4. no assembled IT fails;
5. the baseline `tests` dependencies remain free of `bootstrap`.
