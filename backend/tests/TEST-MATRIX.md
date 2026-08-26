
---

# 2. Nouveau fichier `backend/tests/TEST-MATRIX.md`

Ce second fichier est important : le README définit **les règles**, tandis que la matrice fige **quel test appartient où et comment il est prouvé**.

```markdown
# SIXPAY CONNECT — Test Matrix

## 1. Purpose

This matrix is the normative classification reference for SIXPAY CONNECT
tests.

It defines:

- test type;
- ownership;
- location;
- naming;
- infrastructure;
- CI execution;
- expected evidence.

The matrix complements `backend/tests/README.md`.

The `partner` module remains the golden business-module reference.

---

## 2. Canonical test matrix

| Test type | Ownership | Location | Naming | Infrastructure | CI execution | Expected evidence |
|---|---|---|---|---|---|---|
| Domain unit test | Owning business module | `backend/<module>/src/test/java/com/sixpay/<module>/domain/` | `*Test.java` | JUnit, AssertJ | Default backend gate | Surefire report |
| Application service test | Owning business module | `backend/<module>/src/test/java/com/sixpay/<module>/application/service/` | `*Test.java` | JUnit, Mockito when required, AssertJ | Default backend gate | Surefire report |
| API/controller test | API-owning module | `backend/<module>/src/test/java/com/sixpay/<module>/api/` | `*Test.java` | `@WebMvcTest`, MockMvc, Spring Security Test | Default backend gate | Surefire report |
| Infrastructure adapter test | Adapter-owning module | Mirrored module infrastructure package | `*Test.java` | JUnit/Mockito or focused Spring test | Default backend gate | Surefire report |
| Module database integration | Owning module | `backend/<module>/src/test/java/com/sixpay/<module>/...` | `*IT.java` | Spring, PostgreSQL Testcontainers, Flyway | `-Pfull-tests` | Failsafe report |
| Module integration | Owning module | `backend/<module>/src/test/java/com/sixpay/<module>/...` | `*IT.java` | Spring Boot as required | `-Pfull-tests` | Failsafe report |
| Cross-module integration | `backend/tests` | `backend/tests/src/test/java/com/sixpay/tests/...` | `*IT.java` | Spring Boot, real module beans, Testcontainers | `-Pfull-tests` | Failsafe report |
| Database migration validation | `backend/tests` or migration-owning module | Integration test location | `*IT.java` | Flyway + PostgreSQL Testcontainers | `-Pfull-tests` | Failsafe report |
| Backend authentication integration | `backend/tests` | `com/sixpay/tests/security/` | `*IT.java` | Spring Security + assembled application | `-Pfull-tests` | Failsafe report |
| Backend financial journey | `backend/tests` | `com/sixpay/tests/payment/` or owning scenario package | `*IT.java` | Assembled modules + controlled provider doubles | `-Pfull-tests` | Failsafe + logs/correlation evidence |
| Contract conformance test | Contract/API owner | Owning module or dedicated contract validator | Repository convention | OpenAPI/contract parser + implementation test | Relevant backend/frontend gate | CI result + contract validation output |
| Frontend unit test | Frontend feature/core owner | `frontend/src/**/*.spec.ts` | `*.spec.ts` | Vitest | Frontend quality gate | Coverage/test report |
| Frontend component test | Frontend feature owner | `frontend/src/**/*.spec.ts` | `*.spec.ts` | Vitest + Angular testing utilities | Frontend quality gate | Coverage/test report |
| Frontend standalone E2E | Frontend | `frontend/e2e/` | `*.spec.ts` | Playwright, mock backend mode | Frontend E2E gate | `playwright-report/` |
| Frontend integration-profile E2E | Frontend | `frontend/e2e/` | integration scenario `*.spec.ts` | Playwright network doubles + API frontend profile | Frontend E2E integration gate | `playwright-report-integration/` |
| Accessibility E2E | Frontend | `frontend/e2e/` | `*.spec.ts`, `@a11y` | Playwright + axe | Frontend E2E gate | Playwright report |
| Full-stack pilot E2E | Phase 8 cross-stack ownership | To be introduced in later Phase 8 lot | `*.spec.ts` | Browser + Angular + real SIXPAY backend + PostgreSQL | Future pilot gate | Playwright + backend CI evidence |
| Manual pilot/UAT | Product/business validation | Pilot validation assets | `PILOT-xxx` | Pilot environment | Pilot validation process | Signed/recorded acceptance evidence |

---

## 3. Ownership decision rules

Use the following decision tree.

```text
Does the test validate behavior owned by exactly one module?
│
├── YES
│    │
│    └── Keep the test inside that module.
│
└── NO
     │
     ├── Does it require several backend bounded contexts
     │   or the assembled backend?
     │
     ├── YES → backend/tests
     │
     └── NO
          │
          └── Is it a browser/frontend concern?
               │
               └── frontend