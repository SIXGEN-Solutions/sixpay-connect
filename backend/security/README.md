# Security

The `security` module provides shared authentication, authorization and Spring
Security infrastructure for SIXPAY CONNECT.

`partner` remains the golden reference for testing discipline.

## Dual Authentication — Local + OIDC

Canonical closure evidence:

```text
SECURITY-TEST-COVERAGE.md
DA-10-PASSWORD-LIFECYCLE-CLOSURE.md
DA-11-INTEGRATION-SECURITY-CLOSURE.md
DA-12-DUAL-AUTHENTICATION-CLOSURE.md
```

Final covered boundaries:

```text
LOCAL authentication        COVERED
OIDC authentication         COVERED
Hybrid coexistence          COVERED
Canonical principal         COVERED
SIXPAY authorization        COVERED
Password lifecycle          COVERED
Session security            COVERED
CSRF                        COVERED
Security audit              COVERED
Integration/security tests  COVERED
Documentation gate          COVERED
```

## Final golden gate

From `backend`:

```bash
mvn -pl security \
  -Dtest=DualAuthenticationGoldenGateTest \
  test
```

Critical regression gate:

```bash
mvn -pl security \
  -Dtest=SixpaySecurityAutoConfigurationTest,AuditingAuthenticationEntryPointTest,DualAuthenticationGoldenGateTest \
  test
```

Focused integration/security suite:

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

Frontend:

```bash
cd ../frontend
npm test
npm run build
```

Final classification is valid only when all gates are green:

```text
DUAL AUTHENTICATION — LOCAL + OIDC = CLOSED
```
