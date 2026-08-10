# Security

## Purpose

The `security` module provides shared authentication, authorization and
Spring Security infrastructure for SIXPAY CONNECT.

It is a platform module rather than a business bounded context.

The `partner` module remains the golden reference for testing discipline:
tests stay focused on the layer and responsibility they validate.

## Current implementation

The authoritative branch contains the following core responsibilities:

```text
authentication
  AuthenticatedUser
  CurrentUserProvider
  SecurityContextCurrentUserProvider

authorization
  SixpayRole

jwt
  SixpayJwtAuthoritiesConverter

configuration
  SixpaySecurityAutoConfiguration
```

`SixpaySecurityAutoConfiguration` provides, when no application-specific bean
overrides it:

```text
SixpayJwtAuthoritiesConverter
JwtAuthenticationConverter
CurrentUserProvider
SecurityFilterChain
```

The default OAuth2 filter chain is:

```text
stateless
CSRF disabled
HTTP Basic disabled
form login disabled
/actuator/health/** permitted
all other requests authenticated
OAuth2 Resource Server / JWT enabled
```

Executable applications may override the default filter chain. For example,
`bootstrap` owns profile-specific standalone/development security
configuration.

## Phase 8 — Backend Golden Test Coverage

Detailed evidence is maintained in:

```text
SECURITY-TEST-COVERAGE.md
```

Current classification after 8.2.8:

```text
Core model / policies       COVERED
Application boundary        COVERED
HTTP security infrastructure COVERED
Persistence                 N/A
```

The module does not own a business REST controller or a Security persistence
model requiring a `*PersistenceIT`.

## Validation

From `backend/`:

```bash
mvn --batch-mode --no-transfer-progress     -pl security -am test
```

Then:

```bash
mvn --batch-mode --no-transfer-progress     -pl security -am clean verify
```
