# Security

The `security` module provides shared authentication, authorization and Spring
Security infrastructure for SIXPAY CONNECT.

`partner` remains the golden reference for testing discipline.

## Phase 8 status

```text
Core model / policies        COVERED
Application boundary         COVERED
HTTP security infrastructure COVERED
Persistence                  N/A
```

The focused `SixpaySecurityAutoConfigurationTest` deliberately excludes
DataSource/JPA auto-configuration because the filter-chain test has no database
responsibility.

See:

```text
SECURITY-TEST-COVERAGE.md
```

## Validation

```bash
mvn -pl security     -Dtest=SixpaySecurityAutoConfigurationTest     test
```
