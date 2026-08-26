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

## Customer-owned subscription management

The Customer module owns the implemented subscription capability linking an
enrolled Customer, a Partner and a verified bank account. Canonical contract:
documentation/contracts/internal/customer-subscription-management-api-v1.yaml

Authorities: subscription.read, subscription.create, subscription.update,
subscription.suspend and subscription.close. This capability is distinct from
the deferred TRESOR PAY subscription authorization contracts.
