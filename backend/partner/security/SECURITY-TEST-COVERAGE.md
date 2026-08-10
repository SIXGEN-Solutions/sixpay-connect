# SIXPAY CONNECT — Security Golden Test Coverage

## Phase

```text
Phase 8 — Tests et validation du pilote
Lot 8.2 — Backend Golden Test Coverage
8.2.8 — Security
```

## 1. Repository rules

`ENGINEERING_CONTEXT.md` establishes:

```text
feat/sixpay-test-validate-pilote
```

as the authoritative implementation branch and keeps `partner` as the golden
business-module reference.

Security is a platform module, so the golden checklist is applied according to
its actual responsibilities rather than by inventing business-domain layers.

## 2. Implementation inventory

The current module POM provides:

```text
common
Spring Security
OAuth2 Resource Server
Spring WebMVC
Bean Validation
Spring Data JPA

JUnit
Spring Boot Test
WebMVC Test
Security Test
```

Core implementation verified on the authoritative branch:

```text
AuthenticatedUser
CurrentUserProvider
SecurityContextCurrentUserProvider
SixpayRole
SixpayJwtAuthoritiesConverter
SixpaySecurityAutoConfiguration
```

## 3. Core model / authorization coverage

Existing:

```text
AuthenticatedUserTest
SixpayRoleTest
```

Covered behavior includes:

```text
role recognition
authority recognition
defensive authority copying
blank-subject rejection
ROLE_* canonical naming
```

`AuthenticatedUser` remains framework-independent and therefore behaves like a
small security value object.

Status:

```text
CORE MODEL / POLICIES = COVERED
```

## 4. Authentication application boundary

Existing:

```text
SecurityContextCurrentUserProviderTest
```

Covered:

```text
no authentication -> Optional.empty()
JWT principal -> AuthenticatedUser
subject propagation
preferred_username propagation
authorities propagation
```

`CurrentUserProvider.requireCurrentUser()` also provides the platform boundary
used by business-module authorization policies such as Payment.

Status:

```text
APPLICATION BOUNDARY = COVERED
```

## 5. JWT infrastructure

Existing:

```text
SixpayJwtAuthoritiesConverterTest
```

Covered:

```text
OAuth2 scopes -> SCOPE_*
roles claim -> ROLE_*
role normalization
already-prefixed role remains canonical
scope + role composition
```

Status:

```text
JWT INFRASTRUCTURE = COVERED
```

## 6. Spring Security auto-configuration gap

Production:

```text
SixpaySecurityAutoConfiguration
```

owns the default:

```text
SixpayJwtAuthoritiesConverter bean
JwtAuthenticationConverter bean
CurrentUserProvider bean
SecurityFilterChain
```

Before 8.2.8, focused tests existed for the primitives and JWT converter, but
no test existed at the conventional path:

```text
src/test/java/com/sixpay/security/configuration/
    SixpaySecurityAutoConfigurationTest.java
```

This left the runtime security policy itself without direct behavioral
evidence.

## 7. Phase 8.2.8 addition

8.2.8 adds:

```text
SixpaySecurityAutoConfigurationTest
```

The test uses an explicit Spring Boot test application and MockMvc. It verifies:

```text
default CurrentUserProvider bean
JWT converter beans
/actuator/health is public
protected endpoint without authentication -> 401
protected endpoint with JWT authentication -> 200
```

This validates the filter chain independently of business controllers.

It does not duplicate role/scope conversion tests already owned by
`SixpayJwtAuthoritiesConverterTest`.

Status:

```text
HTTP SECURITY INFRASTRUCTURE = COVERED
```

## 8. API classification

The Security module does not own a business REST API.

Its HTTP responsibility is the shared security filter boundary, not an
application controller.

Therefore:

```text
BUSINESS API = N/A
```

The filter-chain behavior is classified under infrastructure and is tested
with MockMvc.

## 9. Persistence classification

Although the module currently carries the Spring Data JPA dependency, the
verified Security responsibilities for this Phase 8 branch do not establish a
Security-owned persistence aggregate/repository requiring a golden
`SecurityPersistenceIT`.

Phase 8.2.8 does not create an artificial database model merely because JPA is
present in the POM.

Status:

```text
PERSISTENCE = N/A / NO VERIFIED OWNED PERSISTENCE RESPONSIBILITY
```

## 10. Bootstrap ownership boundary

The shared Security module supplies defaults.

`bootstrap` may override the filter chain for executable profiles, including:

```text
standalone
dev
```

This is intentional ownership separation:

```text
security
  -> reusable authentication/authorization defaults

bootstrap
  -> executable/profile-specific security composition
```

Cross-application/profile validation belongs to bootstrap or Phase 8.3, not to
a monolithic Security test.

## 11. Final classification

| Dimension | Status |
|---|---|
| Core model / policies | COVERED |
| Authentication boundary | COVERED |
| JWT conversion | COVERED |
| HTTP security infrastructure | COVERED after 8.2.8 |
| Business API | N/A |
| Persistence | N/A |

Overall:

```text
SECURITY = COVERED FOR CURRENT MODULE RESPONSIBILITIES
```

## 12. Validation commands

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl security -am test
```

Targeted new test:

```bash
mvn -pl security     -Dtest=SixpaySecurityAutoConfigurationTest     test
```

Full module:

```bash
mvn --batch-mode --no-transfer-progress     -pl security -am clean verify
```

## 13. Golden rule

Do not create:

```text
SecurityTestEverything
SecurityFullApplicationTest
```

mixing JWT conversion, business authorization policies and bootstrap profiles.

Use the owning layer:

```text
security primitive
    -> pure unit test

CurrentUserProvider
    -> focused authentication test

JWT claims
    -> converter unit test

default HTTP filter chain
    -> focused MockMvc security test

business-module RBAC/object isolation
    -> owning business module

profile-specific executable security
    -> bootstrap / cross-module validation
```
