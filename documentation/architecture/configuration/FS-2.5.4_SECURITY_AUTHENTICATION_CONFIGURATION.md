# FS-2.5.4 — Security / Authentication Configuration Consolidation

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Purpose

FS-2.5.4 separates authentication runtime composition from Security-owned
policy configuration without changing existing authentication behavior.

## Ownership split

### Bootstrap runtime-owned

Bootstrap owns runtime assembly for:

```text
spring.security.*
server.servlet.session.*
```

This includes:

- OAuth2 resource-server / OIDC provider wiring;
- issuer/JWK runtime endpoints;
- HTTP session timeout;
- session-cookie runtime attributes;
- activation of `local-auth` / `hybrid-auth` profiles.

### Security module-owned

Security owns the semantics, defaults and validation of:

```text
sixpay.security.authentication.*
sixpay.security.local.password.*
```

Current canonical binders are:

```text
AuthenticationCapabilitiesProperties
PasswordPolicyProperties
```

Security therefore owns:

- local authentication enabled/disabled capability;
- OIDC enabled/disabled capability and registration id;
- maximum failed attempts;
- lock duration;
- BCrypt strength;
- password minimum/maximum length;
- password history size;
- password expiration policy.

## Existing profile semantics

`application-local-auth.yml`:

```text
local = enabled
oidc  = disabled
```

`application-hybrid-auth.yml`:

```text
local = enabled
oidc  = enabled
```

These semantics remain unchanged.

## Important distinction

The physical presence of `sixpay.security.*` values in Bootstrap profile YAML
does not make Bootstrap their semantic owner.

```text
Bootstrap YAML
    = runtime value source / profile composition

Security @ConfigurationProperties
    = semantic owner + defaults + validation
```

## Password policy

`PasswordPolicyProperties` validates external values against the Security domain
`PasswordPolicy`.

Therefore Bootstrap must not duplicate password-policy validation or defaults.

## Authentication capability policy

`AuthenticationCapabilitiesProperties` owns the defaults and normalization for:

```text
maximum-failed-attempts = 5
lock-duration           = 15m
bcrypt-strength         = 12
```

Bootstrap profiles may override them through existing environment variables,
but must not introduce competing defaults elsewhere in Java.

## Non-regression rules

FS-2.5.4 does not:

- rename `sixpay.security.*` keys;
- rename auth environment variables;
- change local/OIDC profile semantics;
- change session/cookie defaults;
- change password-policy defaults;
- move Security configuration classes;
- change authentication flows.

## Gate rules

The architecture gate enforces that:

1. Security owns `AuthenticationCapabilitiesProperties`.
2. Security owns `PasswordPolicyProperties`.
3. the canonical `@ConfigurationProperties` prefixes remain stable;
4. local-auth remains local=true / oidc=false;
5. hybrid-auth remains local=true / oidc=true;
6. OAuth2 runtime wiring remains under `spring.security.*`;
7. session runtime configuration remains under `server.servlet.session.*`;
8. no non-Security business module consumes `sixpay.security.*` directly.

## Exit criteria

FS-2.5.4 is DONE when the ownership split is documented and the gate is green,
with no functional authentication behavior changed.
