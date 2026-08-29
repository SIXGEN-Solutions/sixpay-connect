# SIXPAY CONNECT — DA-1 Unified SIXPAY Principal

## 1. Purpose

DA-1 formalizes the single internal authenticated-identity contract used after
authentication and before authorization/business access.

Authoritative revision policy:

```text
TASK_INVOCATION_OR_EXECUTION_ENVIRONMENT
```

The `partner` module remains the golden business-module reference. Authentication
implementation remains owned by `backend/security`.

## 2. Existing implementation reviewed

The authoritative branch already contains a strong foundation:

```text
com.sixpay.security.authentication.AuthenticatedUser
com.sixpay.security.authentication.CurrentUserProvider
com.sixpay.security.authentication.SecurityContextCurrentUserProvider
```

`AuthenticatedUser` is already framework-independent and currently carries:

```text
subject
username
authorities
```

`SecurityContextCurrentUserProvider` already normalizes both:

```text
Jwt authentication
non-Jwt Spring Security authentication
```

into the same `AuthenticatedUser` representation.

DA-1 therefore MUST NOT create a parallel identity model or duplicate current
user resolution.

## 3. Decision

Introduce the explicit canonical contract:

```text
SixpayPrincipal
```

and make the existing:

```text
AuthenticatedUser
```

its concrete implementation.

Target:

```text
Local authentication --------                                                             -> AuthenticatedUser
                              /      implements
OIDC/JWT authentication -----/     SixpayPrincipal
                                      |
                                      v
                                Authorization
                                      |
                                      v
                               Business modules
```

## 4. Canonical contract

DA-1 exposes:

```text
subject
username
roles
permissions
authorities
hasAuthority(...)
```

`authorities` is retained for compatibility with the current authorization
implementation.

`roles` is derived from `ROLE_*` authorities and exposed without the prefix.

`permissions` contains every non-role authority, preserving current scopes such
as:

```text
SCOPE_payment.read
```

without introducing a provider-specific vocabulary.

## 5. Why `userId` is not fabricated in DA-1

The initial design direction proposed:

```java
UUID userId();
```

The authoritative implementation currently has no canonical mapping from:

```text
OIDC issuer + subject
```

to:

```text
internal SIXPAY user UUID
```

That mapping belongs to the Identity Linking lot.

An OIDC `sub` is an opaque provider identifier and is not guaranteed to be a
UUID. Converting it to a synthetic UUID, treating it as an internal user ID, or
making the field nullable would create a false identity invariant.

DA-1 therefore deliberately keeps:

```text
subject
```

as the stable authenticated identity key already supported by the system.

The internal:

```text
UUID userId
```

must be added only when Identity Linking introduces an authoritative SIXPAY-user
resolution source.

This is a deliberate source-of-truth preservation decision, not an omission.

## 6. Compatibility

Existing business code such as:

```text
PaymentAccessPolicy
CurrentUserProvider
AuthenticatedUser.hasRole(...)
AuthenticatedUser.hasAuthority(...)
```

continues to compile and behave unchanged.

No business module must be modified for DA-1.

## 7. Forbidden dependencies

Business modules MUST NOT branch on:

```text
LOCAL
OIDC
JwtAuthenticationToken
UsernamePasswordAuthenticationToken
provider
issuer
```

They continue to consume the normalized security identity and SIXPAY
authorization rules.

## 8. Tests

DA-1 adds focused proof that:

- `AuthenticatedUser` implements `SixpayPrincipal`;
- roles are normalized independently from Spring's `ROLE_` prefix;
- non-role authorities are exposed as permissions;
- derived collections are immutable;
- JWT/OIDC authentication maps to the canonical principal;
- local username/password authentication maps to the same canonical principal;
- both paths expose the same role/permission contract.

## 9. Exit criteria

DA-1 is complete when:

```text
[ ] SixpayPrincipal exists in backend/security
[ ] AuthenticatedUser implements SixpayPrincipal
[ ] existing CurrentUserProvider contract remains compatible
[ ] Local authentication resolves to AuthenticatedUser/SixpayPrincipal
[ ] OIDC/JWT authentication resolves to AuthenticatedUser/SixpayPrincipal
[ ] roles and permissions are mechanism-neutral
[ ] no business module depends on Local/OIDC selection
[ ] security focused tests are green
[ ] backend/security module tests are green
```

## 10. Validation

From `backend/`:

```bash
mvn -pl security \
    -Dtest=AuthenticatedUserTest,SecurityContextCurrentUserProviderTest \
    test
```

Then:

```bash
mvn -pl security -am test
```

## 11. Next lot

```text
DA-2 — Hybrid authentication capability configuration
```

DA-2 may safely build on this canonical identity contract without changing
business-domain behavior.
