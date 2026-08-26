# SIXPAY CONNECT — Payment Module

The authoritative implementation branch is:

```text
feat/repository-baseline-consolidation-cleanup
```

`partner` remains the golden reference for implementation and layered test
structure.

## Phase 8 golden coverage

After the 8.2.9 Application/Infrastructure closure:

```text
Domain          COVERED
Application     COVERED
API             COVERED
Infrastructure  COVERED
```

Overall:

```text
PAYMENT = COVERED
```

Application closure is completed with focused tests for:

```text
SecuredPaymentProjectionQueryService
SearchPaymentProjectionsQuery
```

Infrastructure required no additional generic IT because the branch already
contains focused PostgreSQL/behavioral evidence for:

```text
projection/query + cursor
idempotency + concurrency
schema/migrations
audit atomicity
outbox atomicity
end-to-end module integration
```

Detailed evidence is maintained in:

```text
PAYMENT-TEST-COVERAGE.md
```

## Validation

```bash
mvn -pl payment \
  -Dtest=SecuredPaymentProjectionQueryServiceTest,SearchPaymentProjectionsQueryTest \
  test

mvn -pl payment -am test

mvn -pl payment -am -Pfull-tests clean verify
```
