# Customer

The Customer module contains two principal capability families:

```text
verification
observation
```

`partner` remains the golden module for structure and testing discipline.

## Phase 8 golden coverage

Current classification after the 8.2.9 Observation persistence/query
remediation:

```text
Domain                                  COVERED
Application                             COVERED
API                                     COVERED
Infrastructure — Banking                COVERED
Infrastructure — Observation persistence/query
                                        COVERED
```

Overall:

```text
CUSTOMER = COVERED
```

The PostgreSQL evidence closing the Observation persistence/query gap is:

```text
src/test/java/com/sixpay/customer/observation/infrastructure/persistence/
    ObservedCustomerPersistenceQueryIT.java
```

Detailed evidence is maintained in:

```text
CUSTOMER-TEST-COVERAGE.md
```
