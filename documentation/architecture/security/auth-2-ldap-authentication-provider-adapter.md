# SIXPAY CONNECT — AUTH-2
## LDAP Authentication Provider / Adapter

### Status

```text
AUTH-2 — IMPLEMENTATION PREPARED
```

### Baseline

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Baseline SHA: `732a773616da5be2aedbcf3df6d74945152b472f`
- Prerequisite: AUTH-1 approved Microsoft Active Directory baseline
- Identity linking: **NOT INCLUDED**
- SIXPAY account creation: **NOT INCLUDED**
- SIXPAY authorization assignment: **NOT INCLUDED**

## 1. Scope

AUTH-2 owns only:

```text
LDAP credentials
    -> Microsoft Active Directory authentication
    -> trusted LDAP ExternalIdentity
```

The output contract is:

```text
AuthenticationIdentityType.LDAP
+
ExternalIdentity(
    issuer   = configured trustDomain,
    subject  = stable normalized objectGUID (or configured approved subject attribute),
    username = configured username/display attribute
)
```

`ExternalIdentity` remains provider-neutral; the identity type is carried by the
LDAP authentication port/result boundary rather than by adding a provider field
to `ExternalIdentity`.

## 2. Explicit exclusions

AUTH-2 must not:

- create `SixpayUserAccount`;
- create roles or permissions;
- automatically link a LDAP identity;
- use LDAP groups as authorities;
- persist or expose the user's password after authentication;
- use the full DN as the canonical external subject when a stable subject exists;
- create the SIXPAY backend session;
- introduce business-module LDAP dependencies.

Identity linking belongs to AUTH-3.

## 3. Active Directory authentication sequence

```text
username + password
        |
        v
validate input
        |
        v
service-account LDAPS search
        |
        +--> exactly zero result -> generic authentication failure
        +--> more than one result -> generic authentication failure
        |
        v
read user DN + stable subject attribute + username/display attribute
        |
        v
verify AD account state when exposed/configured
        |
        v
user bind over LDAPS using transient credentials
        |
        +--> failure -> generic authentication failure
        |
        v
normalize stable subject
        |
        v
LdapAuthenticationResult(
    identityType = LDAP,
    externalIdentity = ExternalIdentity(
        issuer = trustDomain,
        subject = stableSubject,
        username = directoryUsername
    )
)
```

The password exists only for the duration of the authentication call and is
never copied into the result, audit event, exception message or persisted state.

## 4. Configuration

Security owns semantics under:

```text
sixpay.security.authentication.ldap.*
```

Approved configuration model:

- `enabled`
- ordered `urls`
- `baseDn`
- `userSearchBase`
- `userSearchFilter`
- `loginAttribute`
- `usernameAttribute`
- `subjectAttribute`
- `trustDomain`
- `serviceAccountDn`
- runtime-injected `serviceAccountPassword`
- `connectTimeout`
- `readTimeout`
- `authenticationTimeout`

Defaults:

```text
userSearchFilter      = (sAMAccountName={0})
loginAttribute        = sAMAccountName
usernameAttribute     = sAMAccountName
subjectAttribute      = objectGUID
trustDomain           = regionale-ldap
connectTimeout        = 3s
readTimeout           = 5s
authenticationTimeout = 10s
```

Endpoint URLs, Base DN, search base and service-account identity remain
environment-specific and therefore have no production values in Git.

## 5. Dependency decision

AUTH-2 uses Spring LDAP through Spring Boot dependency management.

No explicit third-party version is introduced in the Security module. Dependency
version authority remains the imported Spring Boot platform in `sixpay-bom`.

The dependency addition is limited to the Security module.

## 6. Failure behavior

All authentication failures are externally generic.

Internal classification may distinguish:

- invalid credentials;
- user not found;
- ambiguous user search;
- disabled/locked/expired AD account;
- directory unavailable;
- TLS/trust failure;
- timeout;
- invalid configuration.

Directory unavailability is fail-closed.

## 7. Security invariants

```text
LDAP proves identity.
SIXPAY owns authorization.
```

No LDAP group, token, DN, service-account privilege or AD claim can become a
SIXPAY role or permission in AUTH-2.

## 8. Exit criteria

```text
[ ] AUTH-1 approved AD baseline reflected in documentation
[ ] LDAP properties validated
[ ] credentials accepted only through the authentication input boundary
[ ] AD search requires exactly one user
[ ] user credentials verified against AD over LDAPS
[ ] output contains LDAP identity type + ExternalIdentity
[ ] subject uses stable configured AD attribute
[ ] DN is transient and not canonical identity
[ ] password never leaves authentication call/result
[ ] no SIXPAY account creation
[ ] no identity linking
[ ] no role/permission creation
[ ] no LDAP group -> authority mapping
[ ] focused unit tests pass
[ ] Security module tests pass
[ ] repository gates pass
```

## 9. Next lot

```text
AUTH-3 — LDAP Identity Linking
```
