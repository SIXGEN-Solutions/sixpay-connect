# SIXPAY CONNECT — DA-4 OIDC as Second Authentication Provider

## 1. Scope

DA-4 completes the provider-neutral OIDC authentication path while preserving
Local authentication from DA-3.

Authoritative branch:

```text
feat/repository-baseline-consolidation-cleanup
```

The `partner` module remains the golden business-module reference. OIDC remains
owned by `backend/security` and `frontend/core/auth`.

## 2. Existing implementation reviewed

The repository already contains two important OIDC foundations:

1. the Angular frontend uses `angular-auth-oidc-client` and `responseType: code`;
2. the backend `security` module already includes
   `spring-boot-starter-oauth2-resource-server` and conditionally enables JWT
   bearer authentication when `oidc.enabled=true`.

The frontend therefore already owns the browser Authorization Code login and
callback lifecycle. The backend owns bearer-token validation and SIXPAY
principal normalization.

## 3. Important architecture decision: no backend oauth2-client in DA-4

DA-4 deliberately does **not** add:

```text
spring-boot-starter-oauth2-client
```

to `backend/security`.

Adding it now would create a second browser OAuth2/OIDC client in parallel with
the existing Angular client, with two redirect/callback/session ownership
models.

Current repository architecture remains:

```text
Browser
  |
  | Sign in with SSO
  v
Angular OIDC client
  |
  | Authorization Code flow
  v
OIDC Provider
  |
  | frontend callback / tokens
  v
Angular
  |
  | Authorization: Bearer <access-token>
  v
SIXPAY Resource Server
  |
  v
OidcAuthenticationAdapter
  |
  v
ExternalIdentityResolver
  |
  v
AuthenticatedUser implements SixpayPrincipal
```

A future BFF/server-side-login decision could legitimately introduce
`spring-boot-starter-oauth2-client`, but that would be an architectural
migration, not a dependency required by the current SPA design.

## 4. Provider-neutral OIDC adapter

DA-4 adds:

```text
ExternalIdentity
ExternalIdentityResolver
SubjectExternalIdentityResolver
OidcAuthenticationAdapter
OidcAuthenticationToken
```

`OidcAuthenticationAdapter` executes only after the Resource Server has accepted
the JWT through `JwtDecoder`.

It extracts only protocol-level claims:

```text
iss
sub
preferred_username
email
```

and existing SIXPAY authorities/scopes through the current
`SixpayJwtAuthoritiesConverter`.

There is no Entra, Keycloak, Okta or Auth0 class in core security.

## 5. External identity resolution

DA-4 introduces the resolver seam:

```text
OidcAuthenticationAdapter
        |
        v
ExternalIdentityResolver
        |
        v
AuthenticatedUser / SixpayPrincipal
```

The default DA-4 implementation is `SubjectExternalIdentityResolver`, which
preserves the existing JWT subject as the SIXPAY authenticated subject.

DA-5 Identity Linking can replace this bean with an authoritative mapping of:

```text
issuer + external subject
        ->
internal SIXPAY user
```

without modifying the OIDC adapter or business modules.

## 6. Authorization compatibility

DA-4 intentionally reuses the current `SixpayJwtAuthoritiesConverter` so no
existing authorization rule regresses during authentication work.

This means current JWT roles/scopes remain accepted for compatibility.

Ownership of final SIXPAY business roles/permissions remains the DA-6 concern;
DA-4 does not spread provider claims into business modules.

## 7. Local + OIDC coexistence

The shared filter chain keeps:

```text
SessionCreationPolicy.IF_REQUIRED
```

Local authentication may persist a cookie-backed HttpSession.

OIDC API requests continue to use bearer-token authentication through the
Resource Server.

Both paths now converge to a Spring Security principal whose concrete identity
is:

```text
AuthenticatedUser implements SixpayPrincipal
```

`SecurityContextCurrentUserProvider` therefore remains independent from the
authentication mechanism.

## 8. Provider test

`OidcAuthenticationProviderIT` exercises the complete backend bearer path with a
test `JwtDecoder` boundary:

```text
Bearer token
  -> Resource Server filter
  -> JwtDecoder
  -> OidcAuthenticationAdapter
  -> ExternalIdentityResolver
  -> AuthenticatedUser
  -> CurrentUserProvider
  -> protected endpoint
```

This is intentionally provider-neutral and requires no external Entra/Keycloak
instance during the module test suite.

## 9. Files changed by DA-4

```text
backend/security/src/main/java/com/sixpay/security/
├── domain/authentication/ExternalIdentity.java
├── application/port/out/ExternalIdentityResolver.java
├── application/service/SubjectExternalIdentityResolver.java
├── infrastructure/authentication/oidc/
│   ├── OidcAuthenticationAdapter.java
│   └── OidcAuthenticationToken.java
└── configuration/SixpaySecurityAutoConfiguration.java

backend/security/src/test/java/com/sixpay/security/
├── infrastructure/authentication/oidc/OidcAuthenticationAdapterTest.java
└── configuration/OidcAuthenticationProviderIT.java
```

No business module and no frontend production file requires modification in
DA-4.

## 10. Validation

From `backend/`:

```bash
mvn -pl security \
  -Dtest=OidcAuthenticationAdapterTest,OidcAuthenticationProviderIT,SixpaySecurityAutoConfigurationTest,SecurityContextCurrentUserProviderTest \
  test

mvn -pl security -am test
```

Frontend regression:

```bash
cd ../frontend
npm test
npm run build
```

## 11. Exit criteria

```text
[ ] oidc.enabled=true activates Resource Server bearer authentication
[ ] trusted JWT reaches OidcAuthenticationAdapter
[ ] adapter creates provider-neutral ExternalIdentity
[ ] ExternalIdentityResolver produces canonical AuthenticatedUser
[ ] CurrentUserProvider sees the same SixpayPrincipal contract as Local
[ ] Local authentication continues to work
[ ] no Entra/Keycloak/Okta/Auth0 dependency exists in core security
[ ] no business module depends on provider-specific claims/classes
[ ] provider-neutral OIDC integration test passes
[ ] backend regression passes
[ ] frontend regression passes
```

Next lot:

```text
DA-5 — Identity Linking
```
