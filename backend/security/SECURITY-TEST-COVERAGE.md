# SIXPAY CONNECT — Security Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.8 — Security
8.2.9 remediation — Security structural relocation and test isolation
```

## Current classification

```text
Core model / policies        COVERED
Authentication boundary      COVERED
JWT conversion               COVERED
HTTP security infrastructure COVERED
Business API                 N/A
Persistence                  N/A
```

## Canonical location

Security assets belong under:

```text
backend/security/
```

and never under:

```text
backend/partner/security/
```

`partner` is the golden reference, not the parent of sibling modules.

## Spring test isolation correction

`SixpaySecurityAutoConfigurationTest` validates only the shared HTTP security
boundary.

The module POM contains JPA classes, so a broad `@EnableAutoConfiguration`
caused Spring Boot to start:

```text
DataSourceAutoConfiguration
HibernateJpaAutoConfiguration
DataJpaRepositoriesAutoConfiguration
```

The test has no persistence responsibility and no datasource configuration.

The focused test application therefore excludes exactly those DB/JPA
auto-configurations:

```java
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataJpaRepositoriesAutoConfiguration.class
})
```

No H2 database, PostgreSQL container, fake datasource, or persistence fixture
is introduced.

This preserves the golden testing rule:

```text
one test = one responsibility
```

## Validation

From `backend/`:

```bash
mvn -pl security     -Dtest=SixpaySecurityAutoConfigurationTest     test
```

Then:

```bash
mvn -pl security -am test
```

Finally:

```bash
mvn -pl tests     -Dtest=BackendGoldenCoverageGateTest     test
```
