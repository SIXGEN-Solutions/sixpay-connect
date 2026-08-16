# Security

The `security` module provides shared authentication, authorization and Spring
Security infrastructure for SIXPAY CONNECT.

`partner` remains the golden reference for testing discipline.

## Golden security coverage

Canonical evidence:

```text
SECURITY-TEST-COVERAGE.md
DA-10-PASSWORD-LIFECYCLE-CLOSURE.md
DA-11-INTEGRATION-SECURITY-CLOSURE.md
```

DA-11 locks the integrated Dual Authentication — Local + OIDC boundaries:

```text
Capability matrix         COVERED
LOCAL session             COVERED
OIDC integration          COVERED
Hybrid coexistence        COVERED
Authorization             COVERED
CSRF boundary             COVERED
OIDC security audit       COVERED
```

## DA-11 golden gate

From `backend`:

```bash
mvn -pl security \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

Focused DA-11 integration/security suite:

```bash
mvn -pl security \
  -Pfull-tests \
  -Dit.test=AuthenticationCapabilityMatrixIT,LocalAuthenticationSessionIT,OidcAuthenticationProviderIT,HybridAuthenticationIT,SecurityAuthorizationBoundaryIT \
  verify
```

Security module full gate:

```bash
mvn -pl security -Pfull-tests clean verify
```

Full backend gate:

```bash
mvn -Pfull-tests clean verify
```
