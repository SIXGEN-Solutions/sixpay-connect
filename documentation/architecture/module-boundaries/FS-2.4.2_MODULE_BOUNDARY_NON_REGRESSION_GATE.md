# FS-2.4.2 — Module Boundary Non-Regression Gate

**Branch:** `feat/repository-baseline-consolidation`
**Phase:** `FS-2.4 — Dependency and module boundary audit`
**Golden module:** Partner

## Purpose

FS-2.4.2 turns the reviewed FS-2.4.1 business-module dependency baseline into
a permanent non-regression gate.

The gate is intentionally conservative:

```text
existing reviewed public seam
        => allowed

new business-to-business seam
        => fail until explicitly reviewed
```

It does not rename packages or refactor functional code.

## Current reviewed business edges

```text
Partner        -> Security
Customer       -> Security
Payment        -> Security
Administration -> Security
```

No other business-to-business edge is currently approved.

## Explicit Security public surface

Accepted authentication-context contracts:

```text
com.sixpay.security.authentication.CurrentUserProvider
com.sixpay.security.authentication.AuthenticatedUser
com.sixpay.security.authentication.SixpayPrincipal
```

Accepted authorization contracts:

```text
com.sixpay.security.authorization.*
```

Accepted input-port conventions:

```text
com.sixpay.security.application.port.in.*
com.sixpay.security.application.port.input.*
```

Both historical naming conventions are treated as equivalent.

Accepted application response contracts:

```text
com.sixpay.security.application.model.*
```

Concrete Security internals remain private, including
`SecurityContextCurrentUserProvider`, JWT, infrastructure and configuration.

## Forbidden cross-module surfaces

A business module must never import another business module's:

```text
*.infrastructure.*
*.domain.repository.*
*.entity.*
*JpaEntity*
*.application.port.out.*
*.application.port.output.*
*.configuration.*
*.config.*
```

## Exact-edge policy

A future edge such as Payment -> Customer or Accounting -> Payment fails even
if the imported class looks harmless. It must first be explicitly reviewed.

## Cycle and Maven policy

The gate rejects Java cycles and Maven business-module cycles.

Every business Maven compile dependency must correspond to a real reviewed
production Java edge, and every production Java business edge must have its
matching Maven dependency.

This prevents unused coupling from returning.

## Regression policy

FS-2.4 remains classification-first:

```text
detect
  -> inspect
  -> prove violation
  -> minimal targeted correction
  -> module tests
  -> consumer tests
  -> full reactor verification
```

Existing functional behavior is the protected baseline.
