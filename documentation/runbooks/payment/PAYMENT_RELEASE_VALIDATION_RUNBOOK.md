# Payment Phase 3 Release Validation Runbook

## Purpose

Provide one reproducible release gate for the Payment backend after Lots 3.1
through 3.14.

## Mandatory command

Linux or CI:

```bash
bash scripts/validation/validate-payment-phase3.sh
```

Windows:

```powershell
./scripts/validation/validate-payment-phase3.ps1
```

## Required green gates

1. Java 21 and supported Maven version.
2. Maven reactor build for `payment` and dependencies.
3. Unit tests.
4. Architecture tests.
5. OpenAPI contract tests.
6. PostgreSQL/Testcontainers integration tests.
7. Idempotency, replay, atomicity and optimistic-lock tests.
8. Performance/concurrency regression tests.
9. JaCoCo XML and HTML report generation.
10. Required documentation and runbooks committed.
11. GitHub Actions `Payment Phase 3 quality gate` succeeds.

## OpenAPI gate

`PaymentOpenApiContractValidationTest` requires the canonical Payment Query
OpenAPI lifecycle to match `PaymentStatus` exactly.

The contractual source of truth is:

```text
documentation/contracts/internal/payment-query-api-v1.yaml
```

No patch or alignment artifact is part of the release baseline. Any lifecycle
change must be applied directly to the canonical contract and validated by the
OpenAPI contract test.

Do not approve the release while the canonical contract exposes obsolete or
missing lifecycle values.

## Contract governance gate

The current Payment Query contract remains:

```text
approvalStatus: PENDING_APPROVAL
generationPolicy: REFERENCE_ONLY
codeGenerationAllowed: false
```

Technical validation cannot replace business, security and contract-owner
approval. Record those approvals before production activation.

## Coverage gate

The current parent build generates JaCoCo reports but defines no approved
minimum threshold. The final validation workflow therefore requires the report
to exist and publishes it as an artifact.

A numeric threshold must be approved by the engineering team before adding
`jacoco:check`. Do not invent or silently lower a threshold to obtain a green
build.

## Release evidence

Archive:

- unit-test reports;
- integration-test reports;
- JaCoCo report;
- workflow run URL;
- commit SHA;
- approved OpenAPI contract;
- final validation report.

## Failure policy

Any failed command, missing report, contract mismatch or architecture violation
blocks the Payment Phase 3 release.
