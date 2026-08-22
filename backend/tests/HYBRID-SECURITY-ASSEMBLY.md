# SIXPAY CONNECT — Phase 8.3.5 Hybrid Security Assembly

## Decision

No production Security change is required.

The security module already closes and validates Dual Authentication through:

```text
AuthenticationCapabilityMatrixIT
LocalAuthenticationSessionIT
OidcAuthenticationProviderIT
HybridAuthenticationIT
SecurityAuthorizationBoundaryIT
DualAuthenticationGoldenGateTest
```

Phase 8.3.5 therefore does not duplicate those module-owned tests.

## Cross-module gap

`application-assembled-test.yml` intentionally keeps LOCAL and OIDC disabled.
That remains correct for the generic 8.3.1 assembled context.

8.3.5 starts the same full assembled application but overrides:

```text
LOCAL = enabled
OIDC  = enabled
```

It proves:

```text
both capabilities enabled
LOCAL configuration/controller/use case present
OIDC adapter/external identity resolver present
canonical current-user/session services present
exactly one SecurityFilterChain
BearerTokenAuthenticationFilter present
RestrictedLocalSessionFilter present
anonymous LOCAL login reaches validation (400, not 401)
anonymous administration API remains protected (401)
```

## Files intentionally unchanged

```text
backend/security/**
backend/tests/pom.xml
backend/tests/src/test/resources/application-assembled-test.yml
```

## Execution

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress \
  -pl tests -am \
  -Pfull-tests,assembled-tests \
  -Dit.test=HybridSecurityAssemblyIT \
  verify
```

## Anti-regression gates

```bash
mvn --batch-mode --no-transfer-progress \
  -pl security -am \
  -Pfull-tests \
  -DskipITs=false \
  verify

mvn --batch-mode --no-transfer-progress \
  -pl tests -am \
  -Pfull-tests,assembled-tests \
  -Dit.test=AssembledApplicationContextIT \
  verify

mvn --batch-mode --no-transfer-progress \
  -pl tests -am \
  -Pfull-tests \
  -Dit.test=GoldenModuleE2EIT \
  verify
```

## Exit

```text
8.3.5 Hybrid Security Assembly = IMPLEMENTED
```

The sub-lot becomes CLOSED once the targeted test and anti-regression gates are GREEN.
