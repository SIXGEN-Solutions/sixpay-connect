# Traçabilité d’acceptation — Payment Lot 8

| ID | Criterion | Automated or normative evidence |
| --- | --- | --- |
| `PAY-L8-ACC-001` | 76 invariants have named scenarios | `PaymentTraceabilityValidationTest` |
| `PAY-L8-ACC-002` | 38 legal transitions have named scenarios | `PaymentTraceabilityValidationTest` |
| `PAY-L8-ACC-003` | every transition links invariant, operation and event | `PAYMENT_TEST_TRACEABILITY.yaml` |
| `PAY-L8-ACC-004` | future application tests are named and targeted | `PAY-TEST-APP-*` |
| `PAY-L8-ACC-005` | future persistence tests are named and targeted | `PAY-TEST-PERS-*` |
| `PAY-L8-ACC-006` | architecture tests remain permanent | `PaymentArchitectureTest` |
| `PAY-L8-ACC-007` | event confidentiality inspects record components only | robust record-signature regex |
| `PAY-L8-ACC-008` | current and future tests are distinguished | `testStatus` |
| `PAY-L8-ACC-009` | normative identifiers are validated across catalogues | `traceabilityReferencesOnlyNormativeIdentifiers` |
| `PAY-L8-ACC-010` | no Payment domain behavior changed | manifest Lot 8 scope |
