# SIXPAY CONNECT — DA-2 Hybrid Authentication Capability Configuration

## 1. Scope

DA-2 replaces the exclusive authentication-mode configuration model with
independent authentication capabilities.

Authoritative branch:

```text
feat/repository-baseline-consolidation
```

DA-2 changes configuration semantics. It does not implement Local credential
verification (DA-3), OIDC login/provider composition (DA-4), the final hybrid
login UI (DA-7), or normalized hybrid session discovery/logout (DA-8).

## 2. Existing implementation reviewed

Frontend currently uses:

```text
AuthenticationMode = standalone | local | oidc
```

and `AuthenticationService` initializes one branch through a mode switch.

Backend currently has two historical configuration switches:

```text
sixpay.security.authentication-mode
sixpay.security.mode=oauth2
```

The active shared `SecurityFilterChain` is still conditioned on
`sixpay.security.mode=oauth2`.

DA-2 does not silently remove that filter-chain compatibility switch because
doing so would prematurely mix DA-2 with DA-3/DA-4.

## 3. Target model

Frontend:

```text
authentication:
  standalone: boolean
  local.enabled: boolean
  oidc.enabled: boolean
```

Backend:

```yaml
sixpay:
  security:
    authentication:
      local:
        enabled: true
      oidc:
        enabled: true
        registration-id: sixpay
```

## 4. Production matrix

| Local | OIDC | Production result |
|---|---|---|
| false | false | invalid |
| true | false | Local only |
| false | true | OIDC only |
| true | true | Hybrid |

The frontend performs fail-fast validation for production configuration.

## 5. Standalone

`standalone` is retained exclusively for development/demo environments.

It cannot be combined with Local or OIDC capabilities.

The provided development and Netlify demo environments therefore use:

```text
standalone=true
local.enabled=false
oidc.enabled=false
```

## 6. OIDC configuration and secrets

OIDC browser `authority`, `clientId`, and `scope` are public client
configuration, not client secrets.

Backend issuer/JWK configuration continues to come from environment variables.

No client secret, refresh token, access token, password, or authorization code
is committed to Git.

Provider credentials must remain in deployment secret management/Vault.

## 7. Transitional compatibility

DA-2 introduces:

```text
AuthenticationCapabilitiesProperties
```

in `backend/security`.

The existing:

```text
sixpay.security.mode=oauth2
```

condition is deliberately retained until DA-4 composes the final security chain
from capability flags.

This prevents configuration work from changing runtime security behavior before
the concrete authentication adapters are implemented.

## 8. Frontend behavior in DA-2

`AuthenticationService` now exposes:

```text
isStandaloneMode
isLocalEnabled
isOidcEnabled
```

Compatibility aliases:

```text
isLocalMode
isOidcMode
```

are temporarily retained so the current login component continues to compile.

When both Local and OIDC are enabled, OIDC remains the temporary bootstrap
precedence to preserve the existing production behavior.

DA-7 will expose both buttons/forms.

DA-8 will replace bootstrap/logout precedence with active-session-aware
normalization.

## 9. Backend capability profile

DA-2 adds:

```text
application-hybrid-auth.yml
```

to prove that both capabilities can be represented simultaneously without
adding provider secrets.

This profile is a capability declaration, not a claim that DA-3/DA-4/DA-8 are
already complete.

## 10. No-change boundary

No Local/OIDC branching is introduced in:

```text
partner
payment
customer
accounting
reporting
incident
```

Business modules remain independent of the authentication mechanism.

## 11. Validation

Frontend:

```bash
npm test -- --run
npm run build
```

Backend:

```bash
mvn -pl security     -Dtest=AuthenticationCapabilitiesPropertiesTest,SixpaySecurityAutoConfigurationTest     test

mvn -pl security -am test
```

## 12. Exit criteria

DA-2 is complete when:

```text
[ ] exclusive AuthenticationMode type is removed
[ ] Local capability is independently configurable
[ ] OIDC capability is independently configurable
[ ] Local + OIDC can be configured simultaneously
[ ] standalone remains dev/demo-only
[ ] production false/false is rejected by frontend configuration validation
[ ] OIDC public configuration is required when OIDC is enabled
[ ] backend capability properties bind independently
[ ] hybrid backend profile exists without secrets
[ ] existing OAuth2 SecurityFilterChain behavior is preserved
[ ] no business module depends on authentication capability selection
```

## 13. Next lot

```text
DA-3 — Local Authentication
```
